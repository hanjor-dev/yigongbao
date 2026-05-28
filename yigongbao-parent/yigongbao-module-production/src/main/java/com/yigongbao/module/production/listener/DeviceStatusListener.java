package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import java.util.List;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
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
    private final IProductionRecordService recordService;
    private final OrderMainMapper orderMainMapper;

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

        // 空闲 → 占用：打印开始，更新流转卡状态和 order_main
        if (ProductionConstants.DEVICE_STATE_IDLE.equals(oldState)
                && ProductionConstants.DEVICE_STATE_BUSY.equals(newState)) {
            LocalDateTime now = LocalDateTime.now();
            records.forEach(record -> {
                record.setStatus(FlowStatusEnum.PRINTING.getValue());
                record.setCurrentProcess(com.yigongbao.module.production.enums.ProcessTypeEnum.PRINT.getCode());
                record.setPrintStartTime(now);
                recordMapper.updateById(record);
                log.info("设备状态变更触发打印开始: recordId={}, recordNo={}, deviceId={}",
                        record.getId(), record.getRecordNo(), deviceId);
            });
            // 打印开始直接更新 order_main（设备驱动，无用户上下文，不走 Flow）
            OrderMainEntity orderUpdate = new OrderMainEntity();
            orderUpdate.setId(records.get(0).getOrderId());
            orderUpdate.setStatus(FlowStatusEnum.PRINTING.getValue());
            orderMainMapper.updateById(orderUpdate);
        }
        // 占用 → 空闲：打印完成，更新状态为 PRINT_COMPLETED，等待操作员首检确认
        else if (ProductionConstants.DEVICE_STATE_BUSY.equals(oldState)
                && ProductionConstants.DEVICE_STATE_IDLE.equals(newState)) {
            LocalDateTime now = LocalDateTime.now();
            records.forEach(record -> {
                record.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
                record.setCurrentProcess(null);
                record.setPrintFinishTime(now);
                recordMapper.updateById(record);
                log.info("设备状态变更触发打印完成: recordId={}, recordNo={}, deviceId={}",
                        record.getId(), record.getRecordNo(), deviceId);
            });
            // 不在此处触发 COMPLETE_PRINT，由操作员调用首检确认接口后触发
        }
    }
}
