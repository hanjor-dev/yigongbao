package com.yigongbao.module.basic.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.event.DeviceStateChangeEvent;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.module.basic.device.convert.DeviceConvert;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.service.impl.DeviceServiceImpl;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private ProcessingCenterMapper processingCenterMapper;

    @Mock
    private IDeviceStateLogService deviceStateLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectProvider<PrinterDeviceUsageChecker> printerDeviceUsageCheckerProvider;

    @Mock
    private PrinterDeviceUsageChecker printerDeviceUsageChecker;

    @Spy
    @InjectMocks
    private DeviceServiceImpl deviceService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(deviceService, deviceMapper);
    }

    @Test
    void testCreateDevice_Success() {
        CreateDeviceDTO dto = new CreateDeviceDTO();
        dto.setDeviceId("SLA-001");
        dto.setDeviceName("打印机001");

        when(deviceMapper.selectCount(any())).thenReturn(0L);
        when(deviceMapper.insert(any(DeviceEntity.class))).thenAnswer(invocation -> {
            DeviceEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });

        Long id = deviceService.createDevice(dto);

        assertNotNull(id);
        verify(deviceMapper, times(1)).insert(any(DeviceEntity.class));
    }

    @Test
    void testCreateDevice_DuplicateDeviceId() {
        CreateDeviceDTO dto = new CreateDeviceDTO();
        dto.setDeviceId("SLA-001");

        when(deviceMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> {
            deviceService.createDevice(dto);
        });
    }

    @Test
    void testBatchUpdateDeviceStatus_AutoCreate() {
        DeviceStatusPushDTO dto = new DeviceStatusPushDTO();
        dto.setCenterName("武汉嘉一");

        DeviceStatusPushDTO.DeviceStatus deviceStatus = new DeviceStatusPushDTO.DeviceStatus();
        deviceStatus.setId("SLA-001");
        deviceStatus.setState(1);
        dto.setDevices(Arrays.asList(deviceStatus));

        ProcessingCenterEntity center = new ProcessingCenterEntity();
        center.setId(1L);
        center.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectOne(any())).thenReturn(center);
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList());
        when(deviceMapper.insert(any(DeviceEntity.class))).thenAnswer(invocation -> {
            DeviceEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });
        doReturn(true).when(deviceService).saveBatch(anyList());

        deviceService.batchUpdateDeviceStatus(dto);

        verify(deviceService).saveBatch(anyList());
    }

    @Test
    void batchUpdateDeviceStatus_publishesStateChangeAfterDeviceUpdate() {
        DeviceStatusPushDTO dto = new DeviceStatusPushDTO();
        dto.setCenterName("武汉嘉一");
        DeviceStatusPushDTO.DeviceStatus status = new DeviceStatusPushDTO.DeviceStatus();
        status.setId("SLA-001");
        status.setState(1);
        dto.setDevices(Arrays.asList(status));

        ProcessingCenterEntity center = new ProcessingCenterEntity();
        center.setId(1L);
        center.setCenterName("武汉嘉一");
        DeviceEntity existing = new DeviceEntity();
        existing.setId(2L);
        existing.setDeviceId("SLA-001");
        existing.setState(0);

        when(processingCenterMapper.selectOne(any())).thenReturn(center);
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(existing));
        when(deviceMapper.updateById(existing)).thenReturn(1);

        deviceService.batchUpdateDeviceStatus(dto);

        InOrder inOrder = inOrder(deviceMapper, eventPublisher);
        inOrder.verify(deviceMapper).updateById(existing);
        inOrder.verify(eventPublisher).publishEvent(any(DeviceStateChangeEvent.class));
    }

    @Test
    void batchUpdateDeviceStatus_doesNotPublishWhenDeviceUpdateFails() {
        DeviceStatusPushDTO dto = new DeviceStatusPushDTO();
        dto.setCenterName("武汉嘉一");
        DeviceStatusPushDTO.DeviceStatus status = new DeviceStatusPushDTO.DeviceStatus();
        status.setId("SLA-001");
        status.setState(1);
        dto.setDevices(Arrays.asList(status));
        ProcessingCenterEntity center = new ProcessingCenterEntity();
        center.setId(1L);
        DeviceEntity existing = new DeviceEntity();
        existing.setId(2L);
        existing.setDeviceId("SLA-001");
        existing.setState(0);
        when(processingCenterMapper.selectOne(any())).thenReturn(center);
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(existing));
        when(deviceMapper.updateById(existing)).thenReturn(0);

        assertThrows(BusinessException.class, () -> deviceService.batchUpdateDeviceStatus(dto));

        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void batchUpdateDeviceStatus_acceptsAndPersistsEveryPrinterState(int state) {
        DeviceStatusPushDTO dto = statusPush("SLA-001", state);
        ProcessingCenterEntity center = processingCenter();
        DeviceEntity existing = existingDevice(
                "SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), state == 0 ? 1 : 0);

        when(processingCenterMapper.selectOne(any())).thenReturn(center);
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(existing));
        when(deviceMapper.updateById(existing)).thenReturn(1);

        assertTrue(deviceService.batchUpdateDeviceStatus(dto));

        assertEquals(state, existing.getState());
        verify(deviceMapper).updateById(existing);
        verify(deviceStateLogService).saveBatch(anyList());
        verify(eventPublisher).publishEvent(any(DeviceStateChangeEvent.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    @SuppressWarnings("unchecked")
    void batchUpdateDeviceStatus_autoCreatedPrinterPersistsEveryInitialStateWithoutPublishingEvent(int state) {
        DeviceStatusPushDTO dto = statusPush("SLA-001", state);
        when(processingCenterMapper.selectOne(any())).thenReturn(processingCenter());
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList());
        doReturn(true).when(deviceService).saveBatch(anyList());

        assertTrue(deviceService.batchUpdateDeviceStatus(dto));

        ArgumentCaptor<List<DeviceEntity>> createdCaptor = ArgumentCaptor.forClass(List.class);
        verify(deviceService).saveBatch(createdCaptor.capture());
        assertEquals(1, createdCaptor.getValue().size());
        assertEquals(DeviceTypeEnum.PRINTER_SLA.getCode(), createdCaptor.getValue().get(0).getDeviceType());
        assertEquals(state, createdCaptor.getValue().get(0).getState());
        verifyNoInteractions(deviceStateLogService);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 7})
    void batchUpdateDeviceStatus_rejectsInvalidExistingPrinterStateWithoutSideEffects(Integer state) {
        DeviceStatusPushDTO dto = statusPush("SLA-001", state);
        DeviceEntity existing = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(processingCenterMapper.selectOne(any())).thenReturn(processingCenter());
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(existing));

        assertThrows(BusinessException.class, () -> deviceService.batchUpdateDeviceStatus(dto));

        assertEquals(0, existing.getState());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verify(deviceService, never()).saveBatch(anyList());
        verifyNoInteractions(deviceStateLogService);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 7})
    void batchUpdateDeviceStatus_rejectsInvalidAutoCreatedPrinterStateWithoutSideEffects(Integer state) {
        DeviceStatusPushDTO dto = statusPush("SLA-001", state);
        when(processingCenterMapper.selectOne(any())).thenReturn(processingCenter());
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList());

        assertThrows(BusinessException.class, () -> deviceService.batchUpdateDeviceStatus(dto));

        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verify(deviceService, never()).saveBatch(anyList());
        verifyNoInteractions(deviceStateLogService);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deviceConvert_populatesPrinterStateName() {
        DeviceEntity entity = new DeviceEntity();
        entity.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
        entity.setState(2);

        DeviceVO vo = DeviceConvert.toVO(entity);

        assertEquals("打印完成", vo.getStateName());
    }

    @Test
    void deviceConvert_populatesOccupiedNameForNonPrinterStateOne() {
        DeviceEntity entity = new DeviceEntity();
        entity.setDeviceType(DeviceTypeEnum.WASH_CONTAINER.getCode());
        entity.setState(1);

        DeviceVO vo = DeviceConvert.toVO(entity);

        assertEquals("占用", vo.getStateName());
    }

    @Test
    void deviceConvert_doesNotUsePrinterNameForInvalidNonPrinterState() {
        DeviceEntity entity = new DeviceEntity();
        entity.setDeviceType(DeviceTypeEnum.WASH_CONTAINER.getCode());
        entity.setState(2);

        DeviceVO vo = DeviceConvert.toVO(entity);

        assertNull(vo.getStateName());
    }

    @Test
    void batchUpdateDeviceStatus_rejectsWholeMixedBatchBeforeAnyMutation() {
        DeviceStatusPushDTO dto = statusPush(
                deviceStatus("SLA-001", 6),
                deviceStatus("WASH-001", 2));
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        DeviceEntity nonPrinter = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(processingCenterMapper.selectOne(any())).thenReturn(processingCenter());
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(printer, nonPrinter));

        assertThrows(BusinessException.class, () -> deviceService.batchUpdateDeviceStatus(dto));

        assertEquals(0, printer.getState());
        assertEquals(0, nonPrinter.getState());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verify(deviceService, never()).saveBatch(anyList());
        verifyNoInteractions(deviceStateLogService);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void batchUpdateDeviceStatus_acceptsValidMixedBatch() {
        DeviceStatusPushDTO dto = statusPush(
                deviceStatus("SLA-001", 6),
                deviceStatus("WASH-001", 1));
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        DeviceEntity nonPrinter = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(processingCenterMapper.selectOne(any())).thenReturn(processingCenter());
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(printer, nonPrinter));
        when(deviceMapper.updateById(any(DeviceEntity.class))).thenReturn(1);

        assertTrue(deviceService.batchUpdateDeviceStatus(dto));

        assertEquals(6, printer.getState());
        assertEquals(1, nonPrinter.getState());
        verify(deviceMapper, times(2)).updateById(any(DeviceEntity.class));
        verify(deviceStateLogService).saveBatch(anyList());
        verify(eventPublisher, times(2)).publishEvent(any(DeviceStateChangeEvent.class));
    }

    @Test
    void batchUpdateDeviceStatus_stopsBeforeUpdatesWhenAutoCreateBatchFails() {
        DeviceStatusPushDTO dto = statusPush(
                deviceStatus("SLA-NEW", 0),
                deviceStatus("WASH-001", 1));
        DeviceEntity existing = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(processingCenterMapper.selectOne(any())).thenReturn(processingCenter());
        when(deviceMapper.selectList(any())).thenReturn(Arrays.asList(existing));
        doReturn(false).when(deviceService).saveBatch(anyList());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.batchUpdateDeviceStatus(dto));

        assertTrue(exception.getMessage().contains("设备批量创建失败"));
        verify(deviceService).saveBatch(anyList());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verifyNoInteractions(deviceStateLogService);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void updateDeviceState_acceptsEveryPrinterStateAfterLockWhenPrinterIsUnused(int state) {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), state == 0 ? 1 : 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenReturn(false);
        when(deviceMapper.updateById(printer)).thenReturn(1);

        deviceService.updateDeviceState(printer.getId(), state);

        assertEquals(state, printer.getState());
        InOrder order = inOrder(deviceMapper, printerDeviceUsageCheckerProvider, printerDeviceUsageChecker);
        order.verify(deviceMapper).selectByIdForUpdate(printer.getId());
        order.verify(printerDeviceUsageCheckerProvider).getIfAvailable();
        order.verify(printerDeviceUsageChecker).isInUse(printer.getId());
        order.verify(deviceMapper).updateById(printer);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 7})
    void updateDeviceState_rejectsInvalidPrinterStateAfterLock(Integer state) {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(printer.getId(), state));

        assertEquals(ErrorCodeEnum.INVALID_PARAMETER.getCode(), exception.getCode());
        assertEquals(0, printer.getState());
        verify(deviceMapper).selectByIdForUpdate(printer.getId());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verifyNoInteractions(printerDeviceUsageCheckerProvider, printerDeviceUsageChecker,
                deviceStateLogService, eventPublisher);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void updateDeviceState_rejectsEveryManualStateWhenPrinterIsActive(int state) {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(printer.getId(), state));

        assertEquals(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode(), exception.getCode());
        assertEquals(0, printer.getState());
        InOrder order = inOrder(deviceMapper, printerDeviceUsageChecker);
        order.verify(deviceMapper).selectByIdForUpdate(printer.getId());
        order.verify(printerDeviceUsageChecker).isInUse(printer.getId());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verifyNoInteractions(deviceStateLogService, eventPublisher);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void updateDeviceState_acceptsBinaryStateForNonPrinterWithoutUsageChecker(int state) {
        DeviceEntity device = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 1 - state);
        when(deviceMapper.selectByIdForUpdate(device.getId())).thenReturn(device);
        when(deviceMapper.updateById(device)).thenReturn(1);

        deviceService.updateDeviceState(device.getId(), state);

        assertEquals(state, device.getState());
        verifyNoInteractions(printerDeviceUsageCheckerProvider, printerDeviceUsageChecker, eventPublisher);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {2, 3, 4, 5, 6})
    void updateDeviceState_rejectsNonBinaryStateForNonPrinterWithoutUsageChecker(Integer state) {
        DeviceEntity device = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(device.getId())).thenReturn(device);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(device.getId(), state));

        assertEquals(ErrorCodeEnum.INVALID_PARAMETER.getCode(), exception.getCode());
        assertEquals(0, device.getState());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verifyNoInteractions(printerDeviceUsageCheckerProvider, printerDeviceUsageChecker,
                deviceStateLogService, eventPublisher);
    }

    @Test
    void updateDeviceState_failsClosedWhenPrinterUsageCheckerIsMissing() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(printer.getId(), 6));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("打印设备占用检查"));
        assertEquals(0, printer.getState());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verifyNoInteractions(deviceStateLogService, eventPublisher);
    }

    @Test
    void updateDeviceState_failsClosedWithContextWhenPrinterUsageCheckerThrows() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenThrow(new IllegalStateException("query failed"));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(printer.getId(), 6));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("打印设备占用检查"));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(0, printer.getState());
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
        verifyNoInteractions(deviceStateLogService, eventPublisher);
    }

    @Test
    void updateDeviceState_wrapsBusinessExceptionThrownByPrinterUsageChecker() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        BusinessException checkerFailure = new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "checker failed");
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenThrow(checkerFailure);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(printer.getId(), 6));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
        assertSame(checkerFailure, exception.getCause());
        assertTrue(exception.getMessage().contains("deviceId=" + printer.getId()));
        verify(deviceMapper, never()).updateById(any(DeviceEntity.class));
    }

    @Test
    void updateDeviceState_comparesNullOldStateSafely() {
        DeviceEntity device = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), null);
        when(deviceMapper.selectByIdForUpdate(device.getId())).thenReturn(device);
        when(deviceMapper.updateById(device)).thenReturn(1);

        assertDoesNotThrow(() -> deviceService.updateDeviceState(device.getId(), 0));

        verify(deviceStateLogService).save(any());
    }

    @Test
    void updateDeviceState_doesNotWriteLogWhenDatabaseUpdateFails() {
        DeviceEntity device = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(device.getId())).thenReturn(device);
        when(deviceMapper.updateById(device)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(device.getId(), 1));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
        verifyNoInteractions(deviceStateLogService, eventPublisher);
    }

    @Test
    void updateDeviceState_propagatesStateLogWriteFailure() {
        DeviceEntity device = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(device.getId())).thenReturn(device);
        when(deviceMapper.updateById(device)).thenReturn(1);
        doThrow(new IllegalStateException("log failed")).when(deviceStateLogService).save(any());

        assertThrows(IllegalStateException.class, () -> deviceService.updateDeviceState(device.getId(), 1));
    }

    @Test
    void removeDevice_rejectsActivePrinterEvenWhenRealtimeStateIsIdle() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.removeDevice(printer.getId()));

        assertEquals(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode(), exception.getCode());
        InOrder order = inOrder(deviceMapper, printerDeviceUsageChecker);
        order.verify(deviceMapper).selectByIdForUpdate(printer.getId());
        order.verify(printerDeviceUsageChecker).isInUse(printer.getId());
        verify(deviceMapper, never()).deleteById(printer.getId());
    }

    @Test
    void removeDevice_rejectsNonIdlePrinterAfterUsageCheck() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 6);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenReturn(false);

        assertThrows(BusinessException.class, () -> deviceService.removeDevice(printer.getId()));

        verify(printerDeviceUsageChecker).isInUse(printer.getId());
        verify(deviceMapper, never()).deleteById(printer.getId());
    }

    @Test
    void removeDevice_deletesUnusedIdlePrinterAfterLockAndUsageCheck() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenReturn(false);
        when(deviceMapper.deleteById(printer.getId())).thenReturn(1);

        deviceService.removeDevice(printer.getId());

        InOrder order = inOrder(deviceMapper, printerDeviceUsageChecker);
        order.verify(deviceMapper).selectByIdForUpdate(printer.getId());
        order.verify(printerDeviceUsageChecker).isInUse(printer.getId());
        order.verify(deviceMapper).deleteById(printer.getId());
    }

    @Test
    void removeDevice_failsClosedWhenPrinterUsageCheckerIsMissing() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.removeDevice(printer.getId()));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("打印设备占用检查"));
        verify(deviceMapper, never()).deleteById(printer.getId());
    }

    @Test
    void removeDevice_failsClosedWhenPrinterUsageCheckerThrows() {
        DeviceEntity printer = existingDevice("SLA-001", DeviceTypeEnum.PRINTER_SLA.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(printer.getId())).thenReturn(printer);
        when(printerDeviceUsageCheckerProvider.getIfAvailable()).thenReturn(printerDeviceUsageChecker);
        when(printerDeviceUsageChecker.isInUse(printer.getId())).thenThrow(new IllegalStateException("query failed"));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.removeDevice(printer.getId()));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        verify(deviceMapper, never()).deleteById(printer.getId());
    }

    @Test
    void removeDevice_preservesNonPrinterRulesWithoutUsageChecker() {
        DeviceEntity idle = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(idle.getId())).thenReturn(idle);
        when(deviceMapper.deleteById(idle.getId())).thenReturn(1);

        deviceService.removeDevice(idle.getId());

        verify(deviceMapper).deleteById(idle.getId());
        verifyNoInteractions(printerDeviceUsageCheckerProvider, printerDeviceUsageChecker);
    }

    @Test
    void removeDevice_rejectsOccupiedNonPrinterWithoutUsageChecker() {
        DeviceEntity occupied = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 1);
        when(deviceMapper.selectByIdForUpdate(occupied.getId())).thenReturn(occupied);

        assertThrows(BusinessException.class, () -> deviceService.removeDevice(occupied.getId()));

        verify(deviceMapper, never()).deleteById(occupied.getId());
        verifyNoInteractions(printerDeviceUsageCheckerProvider, printerDeviceUsageChecker);
    }

    @Test
    void removeDevice_throwsWhenDeleteFails() {
        DeviceEntity idle = existingDevice("WASH-001", DeviceTypeEnum.WASH_CONTAINER.getCode(), 0);
        when(deviceMapper.selectByIdForUpdate(idle.getId())).thenReturn(idle);
        when(deviceMapper.deleteById(idle.getId())).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> deviceService.removeDevice(idle.getId()));

        assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void lockedLookupMissingDeviceReturnsDataNotFoundForUpdateAndRemove() {
        when(deviceMapper.selectByIdForUpdate(999L)).thenReturn(null);

        BusinessException updateException = assertThrows(
                BusinessException.class, () -> deviceService.updateDeviceState(999L, 0));
        BusinessException removeException = assertThrows(
                BusinessException.class, () -> deviceService.removeDevice(999L));

        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), updateException.getCode());
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), removeException.getCode());
        verify(deviceMapper, times(2)).selectByIdForUpdate(999L);
        verify(deviceMapper, never()).selectById(999L);
    }

    private DeviceStatusPushDTO statusPush(String deviceId, Integer state) {
        return statusPush(deviceStatus(deviceId, state));
    }

    private DeviceStatusPushDTO statusPush(DeviceStatusPushDTO.DeviceStatus... statuses) {
        DeviceStatusPushDTO dto = new DeviceStatusPushDTO();
        dto.setCenterName("武汉嘉一");
        dto.setDevices(Arrays.asList(statuses));
        return dto;
    }

    private DeviceStatusPushDTO.DeviceStatus deviceStatus(String deviceId, Integer state) {
        DeviceStatusPushDTO.DeviceStatus status = new DeviceStatusPushDTO.DeviceStatus();
        status.setId(deviceId);
        status.setState(state);
        return status;
    }

    private ProcessingCenterEntity processingCenter() {
        ProcessingCenterEntity center = new ProcessingCenterEntity();
        center.setId(1L);
        center.setCenterName("武汉嘉一");
        return center;
    }

    private DeviceEntity existingDevice(String deviceId, String deviceType, Integer state) {
        DeviceEntity device = new DeviceEntity();
        device.setId((long) Math.abs(deviceId.hashCode()));
        device.setDeviceId(deviceId);
        device.setDeviceType(deviceType);
        device.setState(state);
        return device;
    }
}
