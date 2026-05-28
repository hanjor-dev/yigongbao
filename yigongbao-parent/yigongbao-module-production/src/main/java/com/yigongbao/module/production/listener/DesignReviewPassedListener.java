package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.event.DesignReviewPassedEvent;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
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
 * 设计审核通过事件监听器
 * 自动为订单的所有数据包创建生产流转卡
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesignReviewPassedListener {

    private final OrderMainMapper orderMainMapper;
    private final DesignPackageMapper designPackageMapper;
    private final DesignPackageFileMapper designPackageFileMapper;
    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessMapper processMapper;
    private final CodeGeneratorService codeGeneratorService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDesignReviewPassed(DesignReviewPassedEvent event) {
        Long orderId = event.getOrderId();
        log.info("监听到设计审核通过事件: orderId={}", orderId);

        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在，跳过流转卡创建: orderId={}", orderId);
            return;
        }

        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId));

        if (packages.isEmpty()) {
            log.warn("订单无数据包，跳过流转卡创建: orderId={}", orderId);
            return;
        }

        int createdCount = 0;
        for (DesignPackageEntity pkg : packages) {
            ProductionRecordEntity record = createProductionRecord(order, pkg);
            int productCount = createProductRecords(record, pkg);
            createProcessRecords(record.getId(), order.getOrderType());

            record.setTotalProductCount(productCount);
            String qrContent = String.format("RECORD:%s|BATCH:%s", record.getRecordNo(), record.getProductionBatchNo());
            record.setQrCodeUrl(qrContent);
            recordMapper.updateById(record);

            createdCount++;
            log.info("创建生产流转卡: recordId={}, recordNo={}, packageId={}, productCount={}",
                record.getId(), record.getRecordNo(), pkg.getId(), productCount);
        }

        log.info("设计审核通过自动创建流转卡完成: orderId={}, packageCount={}, recordCount={}",
            orderId, packages.size(), createdCount);
    }

    private ProductionRecordEntity createProductionRecord(OrderMainEntity order, DesignPackageEntity pkg) {
        String recordNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_RECORD_NO);
        String batchNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_BATCH_NO);

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
        record.setStatus(FlowStatusEnum.DESIGN_REVIEW_PASSED.getValue());
        recordMapper.insert(record);
        return record;
    }

    private int createProductRecords(ProductionRecordEntity record, DesignPackageEntity pkg) {
        List<DesignPackageFileEntity> files = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, pkg.getId()));

        if (files.isEmpty()) {
            return 0;
        }

        for (DesignPackageFileEntity file : files) {
            ProductionProductEntity product = new ProductionProductEntity();
            product.setProductionRecordId(record.getId());
            product.setPrintFileId(file.getId());
            product.setProductNo(codeGeneratorService.generate(ProductionConstants.PRODUCT_NO));
            product.setProductName(file.getFileName());
            product.setFileName(file.getFileName());
            product.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
            productMapper.insert(product);
        }
        return files.size();
    }

    private void createProcessRecords(Long recordId, Integer orderType) {
        List<ProcessTypeEnum> processTypes = new ArrayList<>();
        processTypes.add(ProcessTypeEnum.PRINT);
        if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(orderType)) {
            processTypes.add(ProcessTypeEnum.WASH);
            processTypes.add(ProcessTypeEnum.CURE);
            processTypes.add(ProcessTypeEnum.CLEAN_DRY);
        }
        processTypes.add(ProcessTypeEnum.PACK);

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
