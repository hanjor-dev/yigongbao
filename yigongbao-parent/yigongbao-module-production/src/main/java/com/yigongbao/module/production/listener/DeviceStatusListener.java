package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
    private final IProductionRecordService recordService;

    @EventListener
    public void onDeviceStateChange(DeviceStateChangeEvent event) {
        Long deviceId = event.getDeviceId();
        Integer oldState = event.getOldState();
        Integer newState = event.getNewState();

        ProductionRecordEntity record = recordMapper.selectOne(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
                        .in(ProductionRecordEntity::getStatus,
                                RecordStatusEnum.PENDING_PRINT.getCode(),
                                RecordStatusEnum.PRINTING.getCode())
                        .last("LIMIT 1"));
        if (record == null) {
            log.debug("设备状态变更，未找到关联的生产流转卡: deviceId={}", deviceId);
            return;
        }

        // 空闲 → 占用：打印开始，仅更新流转卡状态
        if (ProductionConstants.DEVICE_STATE_IDLE.equals(oldState)
                && ProductionConstants.DEVICE_STATE_BUSY.equals(newState)) {
            record.setStatus(RecordStatusEnum.PRINTING.getCode());
            recordMapper.updateById(record);
            log.info("设备状态变更触发打印开始: recordId={}, recordNo={}, deviceId={}",
                    record.getId(), record.getRecordNo(), deviceId);
        }
        // 占用 → 空闲：打印完成，更新状态并聚合触发 Flow
        else if (ProductionConstants.DEVICE_STATE_BUSY.equals(oldState)
                && ProductionConstants.DEVICE_STATE_IDLE.equals(newState)) {
            record.setStatus(RecordStatusEnum.PRINT_COMPLETED.getCode());
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    RecordStatusEnum.PRINT_COMPLETED.getCode(), FlowActionEnum.COMPLETE_PRINT);
            log.info("设备状态变更触发打印完成: recordId={}, recordNo={}, deviceId={}",
                    record.getId(), record.getRecordNo(), deviceId);
        }
    }
}
