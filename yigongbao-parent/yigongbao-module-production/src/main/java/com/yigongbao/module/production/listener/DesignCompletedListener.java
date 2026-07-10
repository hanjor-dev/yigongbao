package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.event.DesignCompletedEvent;
import com.yigongbao.common.event.ProductionCardsCreatedEvent;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设计完成事件监听器
 * 自动为订单的所有数据包创建生产流转卡
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesignCompletedListener {

    private final OrderMainMapper orderMainMapper;
    private final DesignPackageMapper designPackageMapper;
    private final DesignProductMapper designProductMapper;
    private final DesignProductFileMapper designProductFileMapper;
    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessMapper processMapper;
    private final ProductMapper baseProductMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 监听设计完成事件，为订单下每个数据包创建流转卡、产品记录和工序记录
     *
     * @param event 设计完成事件
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDesignCompleted(DesignCompletedEvent event) {
        Long orderId = event.getOrderId();

        // 查询订单信息
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在，跳过流转卡创建: orderId={}", orderId);
            return;
        }

        log.info("监听到设计完成事件: orderId={}, orderCode={}, orderType={}",
            orderId, order.getOrderCode(), order.getOrderType());

        // 查询订单下的所有数据包
        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId));

        if (packages.isEmpty()) {
            log.warn("订单无数据包，跳过流转卡创建: orderId={}", orderId);
            return;
        }

        // 为每个数据包按产品大类创建流转卡
        List<Long> createdRecordIds = new ArrayList<>();
        for (DesignPackageEntity pkg : packages) {
            try {
                // 按产品大类分组
                Map<String, List<DesignProductEntity>> groupedByCategory =
                    groupByProductCategory(pkg.getId());

                if (groupedByCategory.isEmpty()) {
                    log.warn("数据包无有效产品，跳过流转卡创建: packageId={}, packageCode={}",
                        pkg.getId(), pkg.getPackageCode());
                    continue;
                }

                // 为每个产品大类创建流转卡
                for (Map.Entry<String, List<DesignProductEntity>> entry : groupedByCategory.entrySet()) {
                    String category = entry.getKey();
                    List<DesignProductEntity> categoryProducts = entry.getValue();

                    // 幂等性检查：检查该数据包+产品大类的流转卡是否已存在
                    ProductionRecordEntity existingRecord = recordMapper.selectOne(
                        new LambdaQueryWrapper<ProductionRecordEntity>()
                            .eq(ProductionRecordEntity::getDesignPackageId, pkg.getId())
                            .eq(ProductionRecordEntity::getProductCategory, category)
                            .last("LIMIT 1"));

                    if (existingRecord != null) {
                        log.info("数据包的该产品大类流转卡已存在，跳过创建: packageId={}, category={}, recordNo={}",
                            pkg.getId(), category, existingRecord.getRecordNo());
                        continue;
                    }

                    // 创建流转卡
                    ProductionRecordEntity record = createProductionRecord(order, pkg, category, categoryProducts);

                    // 创建产品记录
                    int productCount = createProductRecords(record, categoryProducts);

                    // 创建工序记录
                    createProcessRecords(record.getId(), order.getOrderType());

                    // 更新流转卡产品总数和二维码
                    record.setTotalProductCount(productCount);
                    String qrContent = String.format("RECORD:%s|BATCH:%s",
                        record.getRecordNo(), record.getProductionBatchNo());
                    record.setQrCodeUrl(qrContent);
                    recordMapper.updateById(record);

                    createdRecordIds.add(record.getId());

                    log.info("创建生产流转卡: recordNo={}, packageId={}, category={}, productCount={}",
                        record.getRecordNo(), pkg.getId(), category, productCount);
                }

            } catch (Exception e) {
                log.error("创建生产流转卡失败: orderId={}, packageId={}, packageCode={}",
                    orderId, pkg.getId(), pkg.getPackageCode(), e);
                throw e;
            }
        }

        log.info("设计完成自动创建流转卡完成: orderId={}, packageCount={}, recordCount={}",
            orderId, packages.size(), createdRecordIds.size());

        if (!createdRecordIds.isEmpty()) {
            eventPublisher.publishEvent(new ProductionCardsCreatedEvent(this, createdRecordIds));
        }
    }

    /**
     * 创建单张流转卡，生成流转卡编号和生产批号，初始状态为设计完成
     *
     * @param order 订单信息
     * @param pkg 数据包信息
     * @param category 产品大类编码
     * @param designProducts 设计产品列表
     * @return 流转卡实体
     */
    private ProductionRecordEntity createProductionRecord(OrderMainEntity order, DesignPackageEntity pkg,
                                                          String category, List<DesignProductEntity> designProducts) {
        // 生成流转卡编号和生产批号
        String recordNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_RECORD_NO);
        String batchNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_BATCH_NO);

        // 构建流转卡实体
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setRecordNo(recordNo);
        record.setOrderId(order.getId());
        record.setOrderCode(order.getOrderCode());
        record.setOrderType(order.getOrderType());
        record.setDesignPackageId(pkg.getId());
        record.setDesignPackageCode(pkg.getPackageCode());
        record.setProductionBatchNo(batchNo);
        record.setHospitalName(order.getHospitalName());
        record.setHospitalDeptName(order.getHospitalDeptName());
        record.setDoctorName(order.getDoctorName());
        record.setPatientName(order.getPatientName());
        record.setIsUrgent(order.getIsUrgent());
        record.setIsPostal(order.getIsPostal());
        record.setExpectedDeliveryDate(order.getExpectedDeliveryDate());

        // 从设计产品中提取材质信息
        String material = extractMaterialFromDesignProducts(designProducts);
        record.setMaterial(material);

        // 设置产品大类
        record.setProductCategory(category);

        // 设置初始状态为设计完成
        record.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());
        recordMapper.insert(record);

        log.info("创建生产流转卡记录: packageId={}, category={}, recordNo={}",
            pkg.getId(), category, recordNo);

        return record;
    }

    /**
     * 从设计产品中提取材质信息（拼接颜色+材质，多种组合则用顿号分隔）
     *
     * @param designProducts 设计产品列表
     * @return 材质描述
     */
    private String extractMaterialFromDesignProducts(List<DesignProductEntity> designProducts) {
        if (designProducts.isEmpty()) {
            return null;
        }

        // 拼接颜色+材质，去重后用顿号分隔
        Set<String> materialDescriptions = designProducts.stream()
                .map(dp -> {
                    String color = dp.getColorName();
                    String material = dp.getMaterialName();
                    if (material == null || material.isBlank()) {
                        return null;
                    }
                    if (color != null && !color.isBlank()) {
                        return color + material;
                    }
                    return material;
                })
                .filter(desc -> desc != null)
                .collect(Collectors.toSet());

        if (materialDescriptions.isEmpty()) {
            return null;
        }
        return String.join("、", materialDescriptions);
    }

    /**
     * 按设计产品列表创建生产产品记录，按 quantity 字段展开数量，返回总产品数
     *
     * @param record 流转卡实体
     * @param designProducts 设计产品列表
     * @return 总产品数
     */
    private int createProductRecords(ProductionRecordEntity record, List<DesignProductEntity> designProducts) {
        if (designProducts.isEmpty()) {
            return 0;
        }

        int totalCount = 0;
        for (DesignProductEntity dp : designProducts) {
            // 取该产品关联的第一个文件作为打印文件
            DesignProductFileEntity dpFile = designProductFileMapper.selectOne(
                    new LambdaQueryWrapper<DesignProductFileEntity>()
                            .eq(DesignProductFileEntity::getDesignProductId, dp.getId())
                            .orderByAsc(DesignProductFileEntity::getSortOrder)
                            .last("LIMIT 1"));

            // 按数量展开创建产品记录
            int qty = dp.getQuantity() != null && dp.getQuantity() > 0 ? dp.getQuantity() : 1;
            for (int i = 0; i < qty; i++) {
                ProductionProductEntity product = new ProductionProductEntity();
                product.setProductionRecordId(record.getId());
                product.setPrintFileId(dpFile != null ? dpFile.getPackageFileId() : null);
                product.setProductNo(codeGeneratorService.generate(ProductionConstants.PRODUCT_NO));
                product.setProductName(dp.getProductName());
                product.setSpecName(dp.getSpecName());
                product.setCertNo(dp.getCertNo());
                product.setMaterialName(dp.getMaterialName());
                product.setColorName(dp.getColorName());
                product.setFileName(dpFile != null ? dpFile.getPackageFileName() : null);
                product.setStatus(ProductStatusEnum.PENDING.getCode());
                productMapper.insert(product);
            }
            totalCount += qty;
        }
        return totalCount;
    }

    /**
     * 按订单类型创建工序记录：医疗器械5个（打印+清洗+固化+清洁干燥+包装），非医疗器械2个（打印+包装）
     *
     * @param recordId 流转卡ID
     * @param orderType 订单类型
     */
    private void createProcessRecords(Long recordId, Integer orderType) {
        // 根据订单类型确定工序列表
        List<ProcessTypeEnum> processTypes = new ArrayList<>();
        processTypes.add(ProcessTypeEnum.PRINT);
        if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(orderType)) {
            processTypes.add(ProcessTypeEnum.WASH);
            processTypes.add(ProcessTypeEnum.CURE);
            processTypes.add(ProcessTypeEnum.CLEAN_DRY);
        }
        processTypes.add(ProcessTypeEnum.PACK);

        // 创建工序记录
        for (int i = 0; i < processTypes.size(); i++) {
            ProcessTypeEnum pt = processTypes.get(i);
            ProductionProcessEntity process = new ProductionProcessEntity();
            process.setProductionRecordId(recordId);
            process.setProcessType(pt.getCode());
            process.setProcessName(pt.getDesc());
            process.setProcessOrder(i + 1);
            process.setStatus(ProcessStatusEnum.PENDING.getCode());
            processMapper.insert(process);
        }
    }

    /**
     * 按产品大类分组设计产品
     *
     * @param packageId 数据包ID
     * @return 按产品大类分组的设计产品Map，key为产品大类编码（17.1/17.2）
     */
    private Map<String, List<DesignProductEntity>> groupByProductCategory(Long packageId) {
        // 1. 查询数据包下的所有设计产品
        List<DesignProductEntity> designProducts = designProductMapper.selectList(
            new LambdaQueryWrapper<DesignProductEntity>()
                .eq(DesignProductEntity::getPackageId, packageId));

        if (designProducts.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 批量查询产品大类信息
        Set<Long> productIds = designProducts.stream()
            .map(DesignProductEntity::getProductId)
            .collect(Collectors.toSet());
        List<ProductEntity> products = baseProductMapper.selectBatchIds(productIds);

        // 3. 构建 productId -> category 的映射（过滤category为null的产品）
        Map<Long, String> productCategoryMap = products.stream()
            .filter(p -> p.getCategory() != null)
            .collect(Collectors.toMap(ProductEntity::getId, ProductEntity::getCategory));

        // 4. 按产品大类分组（只保留模型类和导板类）
        Map<String, List<DesignProductEntity>> groupedByCategory = designProducts.stream()
            .filter(dp -> {
                String category = productCategoryMap.get(dp.getProductId());
                return "17.1".equals(category) || "17.2".equals(category);
            })
            .collect(Collectors.groupingBy(dp ->
                productCategoryMap.get(dp.getProductId())));

        // 5. 记录被忽略的产品
        long ignoredCount = designProducts.size() -
            groupedByCategory.values().stream().mapToLong(List::size).sum();
        if (ignoredCount > 0) {
            log.warn("数据包包含非模型/导板类产品，已忽略: packageId={}, ignoredCount={}",
                packageId, ignoredCount);
        }

        return groupedByCategory;
    }
}
