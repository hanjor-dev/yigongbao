package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import java.util.List;
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
    @Mock private ProductionProcessMapper processMapper;
    @Mock private IProductionRecordService recordService;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private ProductionProductMapper productMapper;

    @InjectMocks
    private DeviceStatusListener listener;

    @BeforeEach
    void setUp() {
        initTableInfo(ProductionRecordEntity.class);
        initTableInfo(ProductionProcessEntity.class);
        initTableInfo(ProductionProductEntity.class);
        initTableInfo(OrderMainEntity.class);
    }

    @Test
    void onDeviceStateChange_noAssociatedRecord_doesNothing() {
        when(recordMapper.selectList(any())).thenReturn(List.of());

        listener.onDeviceStateChange(event(1L, 0, 1));

        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_idleToBusy_setsPrintingNoFlow() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE, ProductionConstants.DEVICE_STATE_BUSY));

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.PRINTING.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void onDeviceStateChange_busyToIdle_setsPrintCompletedAndTriggersFlow() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_BUSY, ProductionConstants.DEVICE_STATE_IDLE));

        verify(recordMapper).update(isNull(), any());
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void onDeviceStateChange_otherStateCombination_doesNothing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));

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

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }
}
