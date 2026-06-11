package com.yigongbao.module.production.warehouse.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;
import com.yigongbao.module.production.warehouse.service.IWarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 仓储管理服务实现
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements IWarehouseService {

    private final ProductionProductMapper productMapper;
    private final ProductionRecordMapper recordMapper;
    private final IProductionRecordService recordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void warehouseInProduct(Long productId, WarehouseInProductDTO dto) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        if (!ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许入库: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_IN);
        }

        product.setStatus(ProductStatusEnum.WAREHOUSED.getCode());
        product.setWarehouseInTime(LocalDateTime.now());
        product.setWarehouseInUserId(StpUtil.getLoginIdAsLong());
        product.setWarehouseLocation(dto.getLocation());
        product.setWarehouseInRemark(dto.getRemark());
        productMapper.updateById(product);

        log.info("产品入库: productId={}, productNo={}, location={}",
            productId, product.getProductNo(), dto.getLocation());

        checkAndTransferRecordToWarehoused(product.getProductionRecordId());
    }

    private void checkAndTransferRecordToWarehoused(Long recordId) {
        long pendingCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode())
        );

        if (pendingCount > 0) {
            log.info("流转卡下仍有待入库产品: recordId={}, pendingCount={}", recordId, pendingCount);
            return;
        }

        long warehousedCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSED.getCode())
        );

        if (warehousedCount == 0) {
            log.warn("流转卡下没有已入库产品，跳过聚合: recordId={}", recordId);
            return;
        }

        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null || !FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue().equals(record.getStatus())) {
            return;
        }

        record.setStatus(FlowStatusEnum.WAREHOUSED.getValue());
        recordMapper.updateById(record);

        log.info("流转卡全部产品已入库: recordId={}, recordNo={}", recordId, record.getRecordNo());

        recordService.triggerFlowIfAllExact(
            record.getOrderId(),
            FlowStatusEnum.WAREHOUSED.getValue(),
            FlowActionEnum.COMPLETE_WAREHOUSE_IN
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void warehouseOutProduct(Long productId, WarehouseOutProductDTO dto) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        if (!ProductStatusEnum.WAREHOUSED.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许出库: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_OUT);
        }

        product.setStatus(ProductStatusEnum.WAREHOUSE_OUT.getCode());
        product.setWarehouseOutTime(LocalDateTime.now());
        product.setWarehouseOutUserId(StpUtil.getLoginIdAsLong());
        product.setWarehouseOutRemark(dto.getRemark());
        product.setRecipient(dto.getRecipient());
        product.setRecipientPhone(dto.getRecipientPhone());
        productMapper.updateById(product);

        log.info("产品出库: productId={}, productNo={}, recipient={}",
            productId, product.getProductNo(), dto.getRecipient());

        checkAndTransferRecordToWarehouseOut(product.getProductionRecordId());
    }

    private void checkAndTransferRecordToWarehouseOut(Long recordId) {
        long warehousedCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSED.getCode())
        );

        if (warehousedCount > 0) {
            log.info("流转卡下仍有已入库产品: recordId={}, warehousedCount={}", recordId, warehousedCount);
            return;
        }

        long warehouseOutCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSE_OUT.getCode())
        );

        if (warehouseOutCount == 0) {
            log.warn("流转卡下没有已出库产品，跳过聚合: recordId={}", recordId);
            return;
        }

        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null || !FlowStatusEnum.WAREHOUSED.getValue().equals(record.getStatus())) {
            return;
        }

        record.setStatus(FlowStatusEnum.WAREHOUSE_OUT.getValue());
        recordMapper.updateById(record);

        log.info("流转卡全部产品已出库: recordId={}, recordNo={}", recordId, record.getRecordNo());

        recordService.triggerFlowIfAllExact(
            record.getOrderId(),
            FlowStatusEnum.WAREHOUSE_OUT.getValue(),
            FlowActionEnum.COMPLETE_WAREHOUSE_OUT
        );
    }
}
