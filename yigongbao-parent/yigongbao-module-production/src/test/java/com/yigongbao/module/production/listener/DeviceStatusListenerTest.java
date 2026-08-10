package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.time.LocalDateTime;
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
    @Mock private IProductionProcessService processService;

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
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE, ProductionConstants.DEVICE_STATE_BUSY));

        verify(recordMapper).update(isNull(), any());
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void onDeviceStateChange_idleToBusy_whenReleaseWins_doesNotStartPrint() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(0);

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE, ProductionConstants.DEVICE_STATE_BUSY));

        verify(recordMapper).update(isNull(), any());
        verifyNoInteractions(processMapper, productMapper, processService, orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
        verify(recordService, never()).reconcileOrderProductionStatus(any());
    }

    @Test
    void onDeviceStateChange_busyToIdle_setsPrintCompletedAndTriggersFlow() {
        ProductionRecordEntity record = record(1L, 10L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_BUSY, ProductionConstants.DEVICE_STATE_IDLE));

        verify(recordMapper).update(isNull(), any());
        verify(processService).schedulePostProcessing(eq(1L), argThat(time ->
                time != null && time.getNano() == 0));
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void onDeviceStateChange_busyToIdle_advancesEveryAffectedOrder() {
        ProductionRecordEntity first = record(1L, 10L);
        first.setStatus(FlowStatusEnum.PRINTING.getValue());
        ProductionRecordEntity second = record(2L, 20L);
        second.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(first, second));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_BUSY,
                ProductionConstants.DEVICE_STATE_IDLE));

        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).triggerFlowIfAllReach(20L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
        verify(recordService).reconcileOrderProductionStatus(20L);
    }

    @Test
    void onDeviceStateChange_busyToIdle_whenPrintProcessMissing_rollsBackByThrowing() {
        ProductionRecordEntity record = record(1L, 10L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_BUSY,
                        ProductionConstants.DEVICE_STATE_IDLE)));

        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_idleToBusy_advancesEveryAffectedOrder() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L), record(2L, 20L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any()))
                .thenReturn(List.of(pendingProduct(101L)), List.of(pendingProduct(102L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE,
                ProductionConstants.DEVICE_STATE_BUSY));

        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).triggerFlowIfAllReach(20L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
        verify(recordService).reconcileOrderProductionStatus(20L);
    }

    @Test
    void onDeviceStateChange_idleToBusy_whenPrintProcessMissing_rollsBackByThrowing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE,
                        ProductionConstants.DEVICE_STATE_BUSY)));

        verifyNoInteractions(productMapper, orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_idleToBusy_whenNoPendingProduct_rollsBackByThrowing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE,
                        ProductionConstants.DEVICE_STATE_BUSY)));

        verifyNoInteractions(orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_idleToBusy_whenOnlySomeProductsUpdated_rollsBackByThrowing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L), pendingProduct(102L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_IDLE,
                        ProductionConstants.DEVICE_STATE_BUSY)));

        verifyNoInteractions(orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_busyToIdle_whenStatusUpdateLost_doesNotScheduleAgain() {
        ProductionRecordEntity record = record(1L, 10L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(recordMapper.update(isNull(), any())).thenReturn(0);

        listener.onDeviceStateChange(event(1L, ProductionConstants.DEVICE_STATE_BUSY, ProductionConstants.DEVICE_STATE_IDLE));

        verify(processService, never()).schedulePostProcessing(any(), any(LocalDateTime.class));
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
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

    private ProductionProductEntity pendingProduct(Long id) {
        ProductionProductEntity product = new ProductionProductEntity();
        product.setId(id);
        product.setProductionRecordId(1L);
        product.setStatus(com.yigongbao.module.production.enums.ProductStatusEnum.PENDING.getCode());
        return product;
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }
}
