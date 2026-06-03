package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.event.DesignCompletedEvent;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    private final CodeGeneratorService codeGeneratorService;

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

        // 为每个数据包创建流转卡
        int createdCount = 0;
        for (DesignPackageEntity pkg : packages) {
            try {
                ProductionRecordEntity record = createProductionRecord(order, pkg);
                int productCount = createProductRecords(record, pkg);
                createProcessRecords(record.getId(), order.getOrderType());

                // 更新流转卡产品总数和二维码
                record.setTotalProductCount(productCount);
                String qrContent = String.format("RECORD:%s|BATCH:%s", record.getRecordNo(), record.getProductionBatchNo());
                record.setQrCodeUrl(qrContent);
                recordMapper.updateById(record);

                createdCount++;
            } catch (Exception e) {
                log.error("创建生产流转卡失败: orderId={}, packageId={}, packageCode={}",
                    orderId, pkg.getId(), pkg.getPackageCode(), e);
                throw e;
            }
        }

        log.info("设计完成自动创建流转卡完成: orderId={}, packageCount={}, recordCount={}",
            orderId, packages.size(), createdCount);
    }

    /**
     * 创建单张流转卡，生成流转卡编号和生产批号，初始状态为设计完成
     *
     * @param order 订单信息
     * @param pkg 数据包信息
     * @return 流转卡实体
     */
    private ProductionRecordEntity createProductionRecord(OrderMainEntity order, DesignPackageEntity pkg) {
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
        String material = extractMaterialFromDesignProducts(pkg.getId());
        record.setMaterial(material);

        // 设置初始状态为设计完成
        record.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());
        recordMapper.insert(record);
        return record;
    }

    /**
     * 从设计产品中提取材质信息（拼接颜色+材质，多种组合则用顿号分隔）
     *
     * @param packageId 数据包ID
     * @return 材质描述
     */
    private String extractMaterialFromDesignProducts(Long packageId) {
        // 查询数据包下的所有设计产品
        List<DesignProductEntity> designProducts = designProductMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .select(DesignProductEntity::getMaterialName, DesignProductEntity::getColorName));

        if (designProducts.isEmpty()) {
            return null;
        }

        // 拼接颜色+材质，去重后用顿号分隔
        java.util.Set<String> materialDescriptions = designProducts.stream()
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
                .collect(java.util.stream.Collectors.toSet());

        if (materialDescriptions.isEmpty()) {
            return null;
        }
        return String.join("、", materialDescriptions);
    }

    /**
     * 按设计产品列表创建生产产品记录，按 quantity 字段展开数量，返回总产品数
     *
     * @param record 流转卡实体
     * @param pkg 数据包实体
     * @return 总产品数
     */
    private int createProductRecords(ProductionRecordEntity record, DesignPackageEntity pkg) {
        // 查询数据包下的所有设计产品
        List<DesignProductEntity> designProducts = designProductMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, pkg.getId()));

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
                product.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
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
}
