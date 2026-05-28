package com.yigongbao.module.production.listener;

import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 设备状态监听器单元测试
 *
 * @author hanjor
 * @date 2026-05-27
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceStatusListenerTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private IProductionRecordService recordService;

    @InjectMocks
    private DeviceStatusListener listener;

    @Test
    void onDeviceStateChange_noAssociatedRecord_doesNothing() {
        when(recordMapper.selectOne(any())).thenReturn(null);

        listener.onDeviceStateChange(event(1L, 0, 1));

        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_idleToBusy_setsPrintingNoFlow() {
        when(recordMapper.selectOne(any())).thenReturn(record(1L, 10L));

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE, ProductionConstants.DEVICE_STATE_BUSY));

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.PRINTING.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_busyToIdle_setsPrintCompletedAndTriggersFlow() {
        when(recordMapper.selectOne(any())).thenReturn(record(1L, 10L));

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_BUSY, ProductionConstants.DEVICE_STATE_IDLE));

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.PRINT_COMPLETED.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
    }

    @Test
    void onDeviceStateChange_otherStateCombination_doesNothing() {
        when(recordMapper.selectOne(any())).thenReturn(record(1L, 10L));

        listener.onDeviceStateChange(event(1L, 1, 1));

        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    private DeviceStateChangeEvent event(Long deviceId, Integer oldState, Integer newState) {
        return new DeviceStateChangeEvent(this, deviceId, oldState, newState);
    }

    private ProductionRecordEntity record(Long id, Long orderId) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(orderId);
        r.setRecordNo("REC-00" + id);
        return r;
    }
}
