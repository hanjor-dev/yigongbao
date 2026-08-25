package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.PrinterDeviceStateEnum;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        listener.onDeviceStateChange(event(1L,
                PrinterDeviceStateEnum.IDLE.getCode(), PrinterDeviceStateEnum.WORKING.getCode()));

        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void onDeviceStateChange_newWorking_startsOnlyPendingRecordWithExistingSideEffects(int oldState) {
        when(recordMapper.selectList(any())).thenReturn(List.of(recordWithStatus(
                1L, 10L, FlowStatusEnum.PENDING_PRINT)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, oldState, PrinterDeviceStateEnum.WORKING.getCode()));

        verify(recordMapper).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PENDING_PRINT)));
        verify(recordMapper).update(isNull(), argThat(update -> updateHasStatus(update,
                FlowStatusEnum.PENDING_PRINT)));
        verify(processMapper).update(isNull(), any());
        verify(productMapper).selectList(any());
        verify(productMapper).update(isNull(), any());
        verify(orderMainMapper).update(isNull(), any());
        verifyNoInteractions(processService);
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void onDeviceStateChange_newWorking_usesExplicitPrintTimesAndUpdatesExcelContentTime() {
        LocalDateTime printStartTime = LocalDateTime.of(2026, 8, 25, 10, 20, 30);
        LocalDateTime estimatedPrintFinishTime = printStartTime.plusMinutes(45);
        when(recordMapper.selectList(any())).thenReturn(List.of(recordWithStatus(
                1L, 10L, FlowStatusEnum.PENDING_PRINT)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(new DeviceStateChangeEvent(this, 1L,
                PrinterDeviceStateEnum.IDLE.getCode(), PrinterDeviceStateEnum.WORKING.getCode(),
                printStartTime, 45, estimatedPrintFinishTime));

        ArgumentCaptor<LambdaUpdateWrapper> recordUpdateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(recordMapper).update(isNull(), recordUpdateCaptor.capture());
        LambdaUpdateWrapper recordUpdate = recordUpdateCaptor.getValue();
        assertTrue(updateHasValue(recordUpdate, printStartTime), "print_start_time 未写入显式事件时间");
        assertTrue(updateHasValue(recordUpdate, estimatedPrintFinishTime), "print_finish_time 未写入预计结束时间");
        assertTrue(hasContentUpdateTime(recordUpdate), "content_update_time 未更新");
        verify(processMapper).update(isNull(), argThat(update -> updateHasValue(update, printStartTime)));
    }

    @Test
    void onDeviceStateChange_newWorking_oldEventFallsBackToNowWithoutPredictedFinishTime() {
        LocalDateTime before = LocalDateTime.now();
        when(recordMapper.selectList(any())).thenReturn(List.of(recordWithStatus(
                1L, 10L, FlowStatusEnum.PENDING_PRINT)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(),
                PrinterDeviceStateEnum.WORKING.getCode()));

        LocalDateTime after = LocalDateTime.now();
        verify(recordMapper).update(isNull(), argThat(update ->
                hasLocalDateTimeBetween(update, before, after)
                        && !hasPrintFinishTime(update)));
        verify(processMapper).update(isNull(), argThat(update ->
                hasLocalDateTimeBetween(update, before, after)));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void onDeviceStateChange_newWorking_whenConditionalUpdateLoses_doesNotRunStartSideEffects(int oldState) {
        when(recordMapper.selectList(any())).thenReturn(List.of(recordWithStatus(
                1L, 10L, FlowStatusEnum.PENDING_PRINT)));
        when(recordMapper.update(isNull(), any())).thenReturn(0);

        listener.onDeviceStateChange(event(1L, oldState, PrinterDeviceStateEnum.WORKING.getCode()));

        verify(recordMapper).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PENDING_PRINT)));
        verify(recordMapper).update(isNull(), argThat(update -> updateHasStatus(update,
                FlowStatusEnum.PENDING_PRINT)));
        verifyNoInteractions(processMapper, productMapper, processService, orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
        verify(recordService, never()).reconcileOrderProductionStatus(any());
    }

    @Test
    void onDeviceStateChange_duplicateWorkingEvent_runsStartSideEffectsOnce() {
        ProductionRecordEntity record = recordWithStatus(1L, 10L, FlowStatusEnum.PENDING_PRINT);
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(recordMapper.update(isNull(), any())).thenReturn(1, 0);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(),
                PrinterDeviceStateEnum.WORKING.getCode()));
        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.WORKING.getCode()));

        verify(recordMapper, times(2)).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PENDING_PRINT)));
        verify(recordMapper, times(2)).update(isNull(), argThat(update -> updateHasStatus(update,
                FlowStatusEnum.PENDING_PRINT)));
        verify(processMapper, times(1)).update(isNull(), any());
        verify(productMapper, times(1)).selectList(any());
        verify(productMapper, times(1)).update(isNull(), any());
        verify(orderMainMapper, times(1)).update(isNull(), any());
        verifyNoInteractions(processService);
        verify(recordService, times(1)).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService, times(1)).reconcileOrderProductionStatus(10L);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6})
    void onDeviceStateChange_idleToNonWorking_doesNotStartOrRunAnyProductionSideEffect(int newState) {
        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(), newState));

        verifyNoInteractions(recordMapper, processMapper, productMapper, orderMainMapper,
                processService, recordService);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 6})
    void onDeviceStateChange_finishPreviousStateToIdle_completesPrintingRecord(int oldState) {
        ProductionRecordEntity record = recordWithStatus(1L, 10L, FlowStatusEnum.PRINTING);
        stubRecordQueryByStatus(record);
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, oldState, PrinterDeviceStateEnum.IDLE.getCode()));

        verify(recordMapper).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PRINTING)));
        verify(recordMapper).update(isNull(), argThat(update -> updateHasStatus(update,
                FlowStatusEnum.PRINTING)));
        verify(processService).schedulePostProcessing(eq(1L), argThat(time ->
                time != null && time.getNano() == 0));
        verify(processMapper).update(isNull(), any());
        verifyNoInteractions(productMapper, orderMainMapper);
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void onDeviceStateChange_finishEvent_preservesPredictedPrintFinishTimeEverywhere() {
        LocalDateTime predictedPrintFinishTime = LocalDateTime.of(2026, 8, 25, 11, 5, 30);
        ProductionRecordEntity record = recordWithStatus(1L, 10L, FlowStatusEnum.PRINTING);
        record.setPrintFinishTime(predictedPrintFinishTime);
        stubRecordQueryByStatus(record);
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        verify(processService).schedulePostProcessing(1L, predictedPrintFinishTime);
        verify(recordMapper).update(isNull(), argThat(update -> updateHasValue(update,
                predictedPrintFinishTime) && hasContentUpdateTime(update)));
        verify(processMapper).update(isNull(), argThat(update -> updateHasValue(update,
                predictedPrintFinishTime)));
    }

    @Test
    void onDeviceStateChange_finishEvent_withoutPredictedPrintFinishTimeUsesCurrentTime() {
        ProductionRecordEntity record = recordWithStatus(1L, 10L, FlowStatusEnum.PRINTING);
        stubRecordQueryByStatus(record);
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        LocalDateTime before = LocalDateTime.now().withNano(0);

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        LocalDateTime after = LocalDateTime.now().withNano(0);
        ArgumentCaptor<LambdaUpdateWrapper> recordUpdateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        ArgumentCaptor<LambdaUpdateWrapper> processUpdateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        ArgumentCaptor<LocalDateTime> finishTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(recordMapper).update(isNull(), recordUpdateCaptor.capture());
        verify(processMapper).update(isNull(), processUpdateCaptor.capture());
        verify(processService).schedulePostProcessing(eq(1L), finishTimeCaptor.capture());

        LocalDateTime actualFinishTime = finishTimeCaptor.getValue();
        assertTrue(!actualFinishTime.isBefore(before) && !actualFinishTime.isAfter(after),
                "无预测结束时间时应使用当前时间");
        assertEquals(actualFinishTime, valueForUpdateColumn(recordUpdateCaptor.getValue(), "printFinishTime"),
                "production_record.print_finish_time 应与排程时间一致");
        assertEquals(actualFinishTime, valueForUpdateColumn(processUpdateCaptor.getValue(), "endTime"),
                "打印工序 end_time 应与排程时间一致");
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    void onDeviceStateChange_nonFinishPreviousStateToIdle_keepsPrintingRecordUntouched(int oldState) {
        when(recordMapper.selectList(any())).thenReturn(List.of(recordWithStatus(
                1L, 10L, FlowStatusEnum.PRINTING)));

        listener.onDeviceStateChange(event(1L, oldState, PrinterDeviceStateEnum.IDLE.getCode()));

        verifyNoInteractions(recordMapper, processMapper, productMapper, orderMainMapper,
                processService, recordService);
    }

    @ParameterizedTest
    @CsvSource({
            "1, PENDING_PRINT", "1, PRINT_COMPLETED", "1, PRINT_FAILED",
            "2, PENDING_PRINT", "2, PRINT_COMPLETED", "2, PRINT_FAILED",
            "6, PENDING_PRINT", "6, PRINT_COMPLETED", "6, PRINT_FAILED"
    })
    void onDeviceStateChange_finishEventWithNonPrintingRecord_doesNotRunCompletionSideEffects(
            int oldState, FlowStatusEnum recordStatus) {
        ProductionRecordEntity record = recordWithStatus(1L, 10L, recordStatus);
        stubRecordQueryByStatus(record);

        listener.onDeviceStateChange(event(1L, oldState, PrinterDeviceStateEnum.IDLE.getCode()));

        verify(recordMapper).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PRINTING)));
        verify(recordMapper, never()).update(isNull(), any());
        verifyNoInteractions(processMapper, productMapper, orderMainMapper, processService,
                recordService);
    }

    @Test
    void onDeviceStateChange_workingToIdle_advancesEveryAffectedOrder() {
        ProductionRecordEntity first = record(1L, 10L);
        first.setStatus(FlowStatusEnum.PRINTING.getValue());
        ProductionRecordEntity second = record(2L, 20L);
        second.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(first, second));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).triggerFlowIfAllReach(20L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
        verify(recordService).reconcileOrderProductionStatus(20L);
    }

    @Test
    void onDeviceStateChange_workingToIdle_whenPrintProcessMissing_rollsBackByThrowing() {
        ProductionRecordEntity record = record(1L, 10L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                        PrinterDeviceStateEnum.IDLE.getCode())));

        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_newWorking_advancesEveryAffectedOrder() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L), record(2L, 20L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any()))
                .thenReturn(List.of(pendingProduct(101L)), List.of(pendingProduct(102L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(),
                PrinterDeviceStateEnum.WORKING.getCode()));

        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).triggerFlowIfAllReach(20L,
                FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT);
        verify(recordService).reconcileOrderProductionStatus(10L);
        verify(recordService).reconcileOrderProductionStatus(20L);
    }

    @Test
    void onDeviceStateChange_newWorking_whenPrintProcessMissing_rollsBackByThrowing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(),
                        PrinterDeviceStateEnum.WORKING.getCode())));

        verifyNoInteractions(productMapper, orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_newWorking_whenNoPendingProduct_rollsBackByThrowing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L)));
        when(productMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(),
                        PrinterDeviceStateEnum.WORKING.getCode())));

        verifyNoInteractions(orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_newWorking_whenOnlySomeProductsUpdated_rollsBackByThrowing() {
        when(recordMapper.selectList(any())).thenReturn(List.of(record(1L, 10L)));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.selectList(any())).thenReturn(List.of(pendingProduct(101L), pendingProduct(102L)));
        when(productMapper.update(isNull(), any())).thenReturn(1);

        assertThrows(com.yigongbao.common.exception.BusinessException.class,
                () -> listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.IDLE.getCode(),
                        PrinterDeviceStateEnum.WORKING.getCode())));

        verifyNoInteractions(orderMainMapper);
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_workingToIdle_whenStatusUpdateLost_doesNotScheduleAgain() {
        ProductionRecordEntity record = record(1L, 10L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(recordMapper.update(isNull(), any())).thenReturn(0);

        listener.onDeviceStateChange(event(1L,
                PrinterDeviceStateEnum.WORKING.getCode(), PrinterDeviceStateEnum.IDLE.getCode()));

        verify(processService, never()).schedulePostProcessing(any(), any(LocalDateTime.class));
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void onDeviceStateChange_newDeviceWorkingToPrintFinishedToIdle_completesOnlyAtIdle() {
        stubSuccessfulFinish();

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.PRINT_FINISHED.getCode()));
        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.PRINT_FINISHED.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        verifySingleCompletion();
    }

    @Test
    void onDeviceStateChange_legacyDeviceWorkingToIdle_completesOnce() {
        stubSuccessfulFinish();

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        verifySingleCompletion();
    }

    @Test
    void onDeviceStateChange_workingToOfflineToIdle_completesOnlyAfterRecoveryToIdle() {
        stubSuccessfulFinish();

        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.WORKING.getCode(),
                PrinterDeviceStateEnum.OFFLINE.getCode()));
        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.OFFLINE.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        verifySingleCompletion();
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4})
    void onDeviceStateChange_alarmOrPauseToOfflineToIdle_completesOnlyAtFinalIdle(int initialState) {
        stubSuccessfulFinish();

        listener.onDeviceStateChange(event(1L, initialState,
                PrinterDeviceStateEnum.OFFLINE.getCode()));
        listener.onDeviceStateChange(event(1L, PrinterDeviceStateEnum.OFFLINE.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode()));

        verifySingleCompletion();
    }

    @Test
    void onDeviceStateChange_duplicatePrintFinishedToIdleEvent_runsCompletionSideEffectsOnce() {
        ProductionRecordEntity record = recordWithStatus(1L, 10L, FlowStatusEnum.PRINTING);
        stubRecordQueryByStatus(record);
        when(recordMapper.update(isNull(), any())).thenReturn(1, 0);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        DeviceStateChangeEvent event = event(1L, PrinterDeviceStateEnum.PRINT_FINISHED.getCode(),
                PrinterDeviceStateEnum.IDLE.getCode());
        listener.onDeviceStateChange(event);
        listener.onDeviceStateChange(event);

        verify(recordMapper, times(2)).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PRINTING)));
        verify(recordMapper, times(2)).update(isNull(), any());
        verify(processService, times(1)).schedulePostProcessing(eq(1L), any(LocalDateTime.class));
        verify(processMapper, times(1)).update(isNull(), any());
        verify(recordService, times(1)).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService, times(1)).reconcileOrderProductionStatus(10L);
        verifyNoInteractions(productMapper, orderMainMapper);
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

    private ProductionRecordEntity recordWithStatus(Long id, Long orderId, FlowStatusEnum status) {
        ProductionRecordEntity record = record(id, orderId);
        record.setStatus(status.getValue());
        return record;
    }

    private boolean queryHasStatus(Object query, FlowStatusEnum status) {
        if (!(query instanceof LambdaQueryWrapper<?> wrapper)) {
            return false;
        }
        wrapper.getSqlSegment();
        return wrapper.getParamNameValuePairs().containsValue(status.getValue());
    }

    private boolean updateHasStatus(Object update, FlowStatusEnum status) {
        if (!(update instanceof LambdaUpdateWrapper<?> wrapper)) {
            return false;
        }
        wrapper.getSqlSegment();
        return wrapper.getParamNameValuePairs().containsValue(status.getValue());
    }

    private boolean updateHasValue(Object update, LocalDateTime value) {
        if (!(update instanceof LambdaUpdateWrapper<?> wrapper)) {
            return false;
        }
        wrapper.getSqlSegment();
        wrapper.getSqlSet();
        return wrapper.getParamNameValuePairs().containsValue(value);
    }

    private boolean hasLocalDateTimeBetween(Object update, LocalDateTime before, LocalDateTime after) {
        if (!(update instanceof LambdaUpdateWrapper<?> wrapper)) {
            return false;
        }
        wrapper.getSqlSegment();
        wrapper.getSqlSet();
        return wrapper.getParamNameValuePairs().values().stream()
                .filter(LocalDateTime.class::isInstance)
                .map(LocalDateTime.class::cast)
                .anyMatch(value -> !value.isBefore(before) && !value.isAfter(after));
    }

    private boolean hasPrintFinishTime(Object update) {
        if (!(update instanceof LambdaUpdateWrapper<?> wrapper)) {
            return false;
        }
        return wrapper.getSqlSet() != null && wrapper.getSqlSet().contains("printFinishTime");
    }

    private boolean hasContentUpdateTime(Object update) {
        if (!(update instanceof LambdaUpdateWrapper<?> wrapper)) {
            return false;
        }
        return wrapper.getSqlSet() != null && wrapper.getSqlSet().contains("contentUpdateTime");
    }

    private LocalDateTime valueForUpdateColumn(Object update, String column) {
        if (!(update instanceof LambdaUpdateWrapper<?> wrapper) || wrapper.getSqlSet() == null) {
            return null;
        }
        String prefix = column + "=#{ew.paramNameValuePairs.";
        String sqlSet = wrapper.getSqlSet();
        int valueStart = sqlSet.indexOf(prefix);
        if (valueStart < 0) {
            return null;
        }
        valueStart += prefix.length();
        int valueEnd = sqlSet.indexOf('}', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        Object value = wrapper.getParamNameValuePairs().get(sqlSet.substring(valueStart, valueEnd));
        return value instanceof LocalDateTime ? (LocalDateTime) value : null;
    }

    private void stubRecordQueryByStatus(ProductionRecordEntity record) {
        when(recordMapper.selectList(any())).thenAnswer(invocation -> {
            Object query = invocation.getArgument(0);
            if (!queryHasStatus(query, FlowStatusEnum.PRINTING)) {
                throw new AssertionError("打印完成查询必须限定 PRINTING 状态");
            }
            if (FlowStatusEnum.PRINTING.getValue().equals(record.getStatus())) {
                return List.of(record);
            }
            return List.of();
        });
    }

    private void stubSuccessfulFinish() {
        stubRecordQueryByStatus(recordWithStatus(1L, 10L, FlowStatusEnum.PRINTING));
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
    }

    private void verifySingleCompletion() {
        verify(recordMapper, times(1)).selectList(argThat(query -> queryHasStatus(query,
                FlowStatusEnum.PRINTING)));
        verify(recordMapper, times(1)).update(isNull(), any());
        verify(processService, times(1)).schedulePostProcessing(eq(1L), any(LocalDateTime.class));
        verify(processMapper, times(1)).update(isNull(), any());
        verify(recordService, times(1)).triggerFlowIfAllReach(10L,
                FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(recordService, times(1)).reconcileOrderProductionStatus(10L);
        verifyNoInteractions(productMapper, orderMainMapper);
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
