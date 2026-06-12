package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import java.util.List;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备状态监听器
 * 监听打印设备状态变更，更新流转卡状态，并聚合触发 Flow 状态流转
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusListener {

    private final ProductionRecordMapper recordMapper;
    private final ProductionProcessMapper processMapper;
    private final IProductionRecordService recordService;
    private final OrderMainMapper orderMainMapper;
    private final com.yigongbao.module.production.product.mapper.ProductionProductMapper productMapper;

    /** 监听设备状态变更：IDLE→BUSY 触发打印开始并更新流转卡状态；BUSY→IDLE 触发打印完成并聚合推进 Flow */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDeviceStateChange(DeviceStateChangeEvent event) {
        Long deviceId = event.getDeviceId();
        Integer oldState = event.getOldState();
        Integer newState = event.getNewState();

        List<ProductionRecordEntity> records = recordMapper.selectList(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
                        .in(ProductionRecordEntity::getStatus,
                                FlowStatusEnum.PENDING_PRINT.getValue(),
                                FlowStatusEnum.PRINTING.getValue()));
        if (records.isEmpty()) {
            log.debug("设备状态变更，未找到关联的生产流转卡: deviceId={}", deviceId);
            return;
        }

        // 空闲 → 占用：打印开始，更新流转卡状态并触发 Flow
        if (ProductionConstants.DEVICE_STATE_IDLE.equals(oldState)
                && ProductionConstants.DEVICE_STATE_BUSY.equals(newState)) {
            LocalDateTime now = LocalDateTime.now();
            records.forEach(record -> {
                record.setStatus(FlowStatusEnum.PRINTING.getValue());
                record.setCurrentProcess(com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode());
                record.setPrintStartTime(now);
                record.setContentUpdateTime(now);
                recordMapper.updateById(record);
                updatePrintProcessStartTime(record.getId(), now);
                updateProductStatusToInProcess(record.getId());

                log.info("设备状态变更触发打印开始: recordId={}, recordNo={}, deviceId={}",
                        record.getId(), record.getRecordNo(), deviceId);
            });
            Long orderId = records.get(0).getOrderId();
            recordService.triggerFlowIfAllExact(orderId,
                    FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        }
        // 占用 → 空闲：打印完成，更新状态并聚合触发 Flow
        else if (ProductionConstants.DEVICE_STATE_BUSY.equals(oldState)
                && ProductionConstants.DEVICE_STATE_IDLE.equals(newState)) {
            LocalDateTime now = LocalDateTime.now();
            records.forEach(record -> {
                recordMapper.update(null,
                        new LambdaUpdateWrapper<ProductionRecordEntity>()
                                .eq(ProductionRecordEntity::getId, record.getId())
                                .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINT_COMPLETED.getValue())
                                .set(ProductionRecordEntity::getCurrentProcess, null)
                                .set(ProductionRecordEntity::getPrintFinishTime, now)
                                .set(ProductionRecordEntity::getContentUpdateTime, now));
                updatePrintProcessEndTime(record.getId(), now);

                log.info("设备状态变更触发打印完成: recordId={}, recordNo={}, deviceId={}",
                        record.getId(), record.getRecordNo(), deviceId);
            });
            Long orderId = records.get(0).getOrderId();
            recordService.triggerFlowIfAllExact(orderId,
                    FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        }
    }

    /** 更新打印工序开始时间 */
    private void updatePrintProcessStartTime(Long recordId, LocalDateTime startTime) {
        processMapper.update(null,
                new LambdaUpdateWrapper<com.yigongbao.module.production.process.entity.ProductionProcessEntity>()
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProductionRecordId, recordId)
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProcessType,
                                com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getStatus,
                                ProcessStatusEnum.IN_PROGRESS.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getStartTime, startTime));
    }

    /** 更新打印工序结束时间 */
    private void updatePrintProcessEndTime(Long recordId, LocalDateTime endTime) {
        processMapper.update(null,
                new LambdaUpdateWrapper<com.yigongbao.module.production.process.entity.ProductionProcessEntity>()
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProductionRecordId, recordId)
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProcessType,
                                com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getStatus,
                                ProcessStatusEnum.COMPLETED.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getEndTime, endTime));
    }

    /** 更新流转卡下所有待生产产品状态为生产中 */
    private void updateProductStatusToInProcess(Long recordId) {
        productMapper.update(null,
                new LambdaUpdateWrapper<com.yigongbao.module.production.product.entity.ProductionProductEntity>()
                        .eq(com.yigongbao.module.production.product.entity.ProductionProductEntity::getProductionRecordId, recordId)
                        .eq(com.yigongbao.module.production.product.entity.ProductionProductEntity::getStatus,
                                com.yigongbao.module.production.enums.ProductStatusEnum.PENDING.getCode())
                        .set(com.yigongbao.module.production.product.entity.ProductionProductEntity::getStatus,
                                com.yigongbao.module.production.enums.ProductStatusEnum.IN_PROCESS.getCode()));
    }
}
