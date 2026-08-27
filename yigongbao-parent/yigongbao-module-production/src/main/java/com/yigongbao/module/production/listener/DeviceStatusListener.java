package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.PrinterDeviceStateEnum;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    private static final Set<Integer> PRINT_FINISH_PREVIOUS_STATES = Set.of(
            PrinterDeviceStateEnum.WORKING.getCode(),
            PrinterDeviceStateEnum.PRINT_FINISHED.getCode(),
            PrinterDeviceStateEnum.OFFLINE.getCode());

    private final ProductionRecordMapper recordMapper;
    private final ProductionProcessMapper processMapper;
    private final IProductionRecordService recordService;
    private final IProductionProcessService processService;
    private final OrderMainMapper orderMainMapper;
    private final com.yigongbao.module.production.product.mapper.ProductionProductMapper productMapper;

    /** 监听设备状态变更：进入工作中触发打印开始；合法前置状态→空闲触发打印完成并聚合推进 Flow */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDeviceStateChange(DeviceStateChangeEvent event) {
        Long deviceId = event.getDeviceId();
        Integer oldState = event.getOldState();
        Integer newState = event.getNewState();

        // 进入工作中：打印开始，只查询待打印的流转卡
        if (isPrintStart(newState)) {
            List<ProductionRecordEntity> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ProductionRecordEntity>()
                            .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
                            .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PENDING_PRINT.getValue()));
            if (records.isEmpty()) {
                log.debug("设备开始占用，未找到待打印的流转卡: deviceId={}", deviceId);
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime printStartTime = event.getPrintStartTime() != null
                    ? event.getPrintStartTime() : now;
            LocalDateTime estimatedPrintFinishTime = resolveEstimatedPrintFinishTime(event, printStartTime);
            List<ProductionRecordEntity> startedRecords = new java.util.ArrayList<>();
            records.forEach(record -> {
                LambdaUpdateWrapper<ProductionRecordEntity> updateWrapper = new LambdaUpdateWrapper<ProductionRecordEntity>()
                                .eq(ProductionRecordEntity::getId, record.getId())
                                .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PENDING_PRINT.getValue())
                                .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
                                .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINTING.getValue())
                                .set(ProductionRecordEntity::getCurrentProcess,
                                        com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode())
                                .set(ProductionRecordEntity::getPrintStartTime, printStartTime)
                                .set(ProductionRecordEntity::getContentUpdateTime, now);
                if (estimatedPrintFinishTime != null) {
                    updateWrapper.set(ProductionRecordEntity::getPrintFinishTime, estimatedPrintFinishTime);
                }
                int updated = recordMapper.update(null, updateWrapper);
                if (updated == 0) {
                    log.info("打印开始事件状态更新未生效，跳过重复或已释放记录: recordId={}, deviceId={}",
                            record.getId(), deviceId);
                    return;
                }
                updatePrintProcessStartTime(record.getId(), printStartTime);
                updateProductStatusToInProcess(record.getId());
                startedRecords.add(record);

                log.info("设备状态变更触发打印开始: recordId={}, recordNo={}, deviceId={}",
                        record.getId(), record.getRecordNo(), deviceId);
            });
            if (startedRecords.isEmpty()) {
                return;
            }
            startedRecords.stream()
                    .map(ProductionRecordEntity::getOrderId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(orderId -> {
                        updateOrderProductionStartTime(orderId, now);
                        recordService.triggerFlowIfAllReach(orderId,
                                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
                        recordService.reconcileOrderProductionStatus(orderId);
                    });
        }
        // 工作中/打印完成/离线 → 空闲：打印完成，只查询打印中的流转卡
        else if (isPrintFinish(oldState, newState)) {
            List<ProductionRecordEntity> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ProductionRecordEntity>()
                            .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
                            .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINTING.getValue()));
            if (records.isEmpty()) {
                log.debug("设备变为空闲，未找到打印中的流转卡: deviceId={}", deviceId);
                return;
            }
            LocalDateTime now = LocalDateTime.now().withNano(0);
            List<ProductionRecordEntity> completedRecords = new java.util.ArrayList<>();
            for (ProductionRecordEntity record : records) {
                // print_finish_time 在打印开始时已保存预计结束时间，完成事件只推进状态，不能覆盖该时间基准。
                LocalDateTime estimatedPrintFinishTime = record.getPrintFinishTime() != null
                        ? record.getPrintFinishTime().withNano(0) : now;
                LambdaUpdateWrapper<ProductionRecordEntity> updateWrapper =
                        new LambdaUpdateWrapper<ProductionRecordEntity>()
                                .eq(ProductionRecordEntity::getId, record.getId())
                                .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINTING.getValue())
                                .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINT_COMPLETED.getValue())
                                .set(ProductionRecordEntity::getCurrentProcess, null)
                                .set(ProductionRecordEntity::getContentUpdateTime, now);
                if (record.getPrintFinishTime() == null) {
                    // 兼容未提供预计耗时的旧设备，至少保留一个可用于后续排程的结束时间。
                    updateWrapper.set(ProductionRecordEntity::getPrintFinishTime, estimatedPrintFinishTime);
                }
                int updated = recordMapper.update(null, updateWrapper);
                if (updated == 0) {
                    log.info("打印完成事件状态更新未生效，跳过重复处理: recordId={}, deviceId={}",
                            record.getId(), deviceId);
                    continue;
                }
                processService.schedulePostProcessing(record.getId(), estimatedPrintFinishTime);
                updatePrintProcessEndTime(record.getId(), estimatedPrintFinishTime);
                completedRecords.add(record);

                log.info("设备状态变更触发打印完成: recordId={}, recordNo={}, deviceId={}",
                        record.getId(), record.getRecordNo(), deviceId);
            }
            if (completedRecords.isEmpty()) {
                return;
            }
            completedRecords.stream()
                    .map(ProductionRecordEntity::getOrderId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(orderId -> {
                        recordService.triggerFlowIfAllReach(orderId,
                                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
                        recordService.reconcileOrderProductionStatus(orderId);
                    });
        }
    }

    private boolean isPrintStart(Integer newState) {
        return PrinterDeviceStateEnum.WORKING.getCode().equals(newState);
    }

    private boolean isPrintFinish(Integer oldState, Integer newState) {
        return PrinterDeviceStateEnum.IDLE.getCode().equals(newState)
                && PRINT_FINISH_PREVIOUS_STATES.contains(oldState);
    }

    /** 获取设备事件中的预计完成时间；兼容只传预计时长的事件。 */
    private LocalDateTime resolveEstimatedPrintFinishTime(DeviceStateChangeEvent event,
                                                          LocalDateTime printStartTime) {
        if (event.getEstimatedPrintFinishTime() != null) {
            return event.getEstimatedPrintFinishTime();
        }
        Integer estimatedDurationMinutes = event.getEstimatedDurationMinutes();
        if (estimatedDurationMinutes == null || estimatedDurationMinutes < 0) {
            return null;
        }
        try {
            return printStartTime.plusMinutes(estimatedDurationMinutes);
        } catch (DateTimeException exception) {
            log.warn("计算预计打印结束时间失败，忽略该字段: printStartTime={}, minutes={}",
                    printStartTime, estimatedDurationMinutes);
            return null;
        }
    }

    /** 更新打印工序开始时间 */
    private void updatePrintProcessStartTime(Long recordId, LocalDateTime startTime) {
        int updated = processMapper.update(null,
                new LambdaUpdateWrapper<com.yigongbao.module.production.process.entity.ProductionProcessEntity>()
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProductionRecordId, recordId)
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProcessType,
                                com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getStatus,
                                ProcessStatusEnum.IN_PROGRESS.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getStartTime, startTime));
        if (updated != 1) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL,
                    "打印工序记录缺失或重复，无法开始打印");
        }
    }

    /** 更新打印工序结束时间 */
    private void updatePrintProcessEndTime(Long recordId, LocalDateTime endTime) {
        int updated = processMapper.update(null,
                new LambdaUpdateWrapper<com.yigongbao.module.production.process.entity.ProductionProcessEntity>()
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProductionRecordId, recordId)
                        .eq(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getProcessType,
                                com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getStatus,
                                ProcessStatusEnum.COMPLETED.getCode())
                        .set(com.yigongbao.module.production.process.entity.ProductionProcessEntity::getEndTime, endTime));
        if (updated != 1) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL,
                    "打印工序记录缺失或重复，无法完成打印");
        }
    }

    /** 更新流转卡下所有待生产产品状态为生产中 */
    private void updateProductStatusToInProcess(Long recordId) {
        List<com.yigongbao.module.production.product.entity.ProductionProductEntity> products =
                productMapper.selectList(
                        new LambdaQueryWrapper<com.yigongbao.module.production.product.entity.ProductionProductEntity>()
                                .eq(com.yigongbao.module.production.product.entity.ProductionProductEntity::getProductionRecordId,
                                        recordId)
                                .select(com.yigongbao.module.production.product.entity.ProductionProductEntity::getId)
                                .last("FOR UPDATE"));
        if (products == null || products.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL,
                    "流转卡没有生产产品，无法开始打印");
        }
        int updated = productMapper.update(null,
                new LambdaUpdateWrapper<com.yigongbao.module.production.product.entity.ProductionProductEntity>()
                        .eq(com.yigongbao.module.production.product.entity.ProductionProductEntity::getProductionRecordId, recordId)
                        .eq(com.yigongbao.module.production.product.entity.ProductionProductEntity::getStatus,
                                com.yigongbao.module.production.enums.ProductStatusEnum.PENDING.getCode())
                        .set(com.yigongbao.module.production.product.entity.ProductionProductEntity::getStatus,
                                com.yigongbao.module.production.enums.ProductStatusEnum.IN_PROCESS.getCode()));
        if (updated != products.size()) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL,
                    "流转卡产品状态不一致，无法开始打印");
        }
    }

    /** 更新订单生产开始时间（仅当为空时更新） */
    private void updateOrderProductionStartTime(Long orderId, LocalDateTime startTime) {
        int updated = orderMainMapper.update(null,
                new LambdaUpdateWrapper<OrderMainEntity>()
                        .eq(OrderMainEntity::getId, orderId)
                        .isNull(OrderMainEntity::getProductionStartTime)
                        .set(OrderMainEntity::getProductionStartTime, startTime));
        if (updated > 0) {
            log.info("更新订单生产开始时间: orderId={}, productionStartTime={}", orderId, startTime);
        }
    }
}
