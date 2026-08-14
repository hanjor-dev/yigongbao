package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import com.yigongbao.module.production.helper.ProductLedgerExcelBuilder;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.service.IProductNumberService;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.AssignProductWeightDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.PrinterAvailabilityService;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionRecordAssignDeviceTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignPackageFileMapper designPackageFileMapper;
    @Mock private DesignInstructionMapper designInstructionMapper;
    @Mock private DesignDrawingMapper designDrawingMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private UserHospitalService userHospitalService;
    @Mock private FlowFacade flowFacade;
    @Mock private FlowCardExcelBuilder flowCardExcelBuilder;
    @Mock private ProductLedgerExcelBuilder productLedgerExcelBuilder;
    @Mock private FileService fileService;
    @Mock private ConfigService configService;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private IDeviceUsageCounterService deviceUsageCounterService;
    @Mock private IProductNumberService productNumberService;
    @Mock private ProductionProcessMapper processMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private PrinterDeviceUsageChecker usageChecker;
    @Mock private ObjectProvider<PrinterDeviceUsageChecker> usageCheckerProvider;
    @Mock private ObjectProvider<PrinterAvailabilityService> availabilityServiceProvider;

    private PrinterAvailabilityService availabilityService;

    @Spy
    @InjectMocks
    private ProductionRecordServiceImpl service;

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProductionRecordEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProductionProcessEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ProductionProductEntity.class);
    }

    @BeforeEach
    void setBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
        availabilityService = spy(new PrinterAvailabilityService(usageChecker));
        lenient().when(usageCheckerProvider.getObject()).thenReturn(usageChecker);
        lenient().when(availabilityServiceProvider.getObject()).thenReturn(availabilityService);
        ReflectionTestUtils.setField(service, "printerDeviceUsageCheckerProvider", usageCheckerProvider);
        ReflectionTestUtils.setField(service, "printerAvailabilityServiceProvider", availabilityServiceProvider);
    }

    @Test
    void assignDevice_rejectsRecordNotFound() {
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(readyDevice(2L));
        when(usageChecker.isInUse(2L)).thenReturn(false);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignDevice(1L, dto));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode());
        InOrder order = inOrder(deviceMapper, usageChecker, availabilityService, recordMapper);
        order.verify(deviceMapper).selectByIdForUpdate(2L);
        order.verify(usageChecker).isInUse(2L);
        order.verify(availabilityService).requireAvailable(any(DeviceEntity.class), eq(false));
        order.verify(recordMapper).selectByIdForUpdate(1L);
        verify(recordMapper, never()).selectById(any());
        verifyNoInteractions(processMapper, productNumberService);
    }

    @Test
    void assignDevice_rejectsMissingDeviceBeforeLookingUpRecord() {
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignDevice(1L, dto));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.PRINT_DEVICE_NOT_FOUND.getCode());
        verifyNoInteractions(usageChecker, availabilityService);
        verifyNoInteractions(recordMapper);
    }

    @Test
    void assignDevice_rejectsOfflineDevice() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = new DeviceEntity();
        device.setId(2L);
        device.setState(0);
        device.setConnectionStatus(0);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignDevice(1L, dto));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
        verify(recordMapper, never()).selectByIdForUpdate(any());
        verifyNoInteractions(processMapper, productNumberService);
    }

    @Test
    void assignDevice_successUpdatesProcessAndGeneratesFormalNumbers() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = new DeviceEntity();
        device.setId(2L);
        device.setDeviceId("SLA-002");
        device.setDeviceName("打印机2");
        device.setState(0);
        device.setConnectionStatus(1);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        dto.setMaterial("树脂");
        ProductionProductEntity product1 = product(11L);
        ProductionProductEntity product2 = product(12L);
        product2.setWeight(new BigDecimal("5.00"));
        dto.setProductWeights(List.of(weight(11L, "12.35"), weight(12L, null)));
        UserEntity user = new UserEntity();
        user.setRealName("生产员");
        user.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.ADMIN.getCode());
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        doReturn(true).when(service).updateById(any(ProductionRecordEntity.class));
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(deviceUsageCounterService.incrementAndGet(2L)).thenReturn(3);
        when(productMapper.selectList(any())).thenReturn(List.of(product1, product2));
        when(productMapper.updateById(any(ProductionProductEntity.class))).thenReturn(1);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            service.assignDevice(1L, dto);
        }

        assertThat(record.getPrintDeviceId()).isEqualTo(2L);
        assertThat(record.getPrintDeviceCode()).isEqualTo("SLA-002");
        assertThat(product1.getWeight()).isEqualByComparingTo("12.35");
        assertThat(product2.getWeight()).isNull();
        verify(productMapper).updateById(product1);
        verify(productMapper).updateById(product2);
        verify(processMapper).update(isNull(), any());
        verify(deviceUsageCounterService).incrementAndGet(2L);
        verify(productNumberService).generateFormalNumbers(1L, 2L, 3);
        InOrder lockOrder = inOrder(deviceMapper, usageChecker, availabilityService, recordMapper, service);
        lockOrder.verify(deviceMapper).selectByIdForUpdate(2L);
        lockOrder.verify(usageChecker).isInUse(2L);
        lockOrder.verify(availabilityService).requireAvailable(device, false);
        lockOrder.verify(recordMapper).selectByIdForUpdate(1L);
        lockOrder.verify(service).updateById(record);
        verify(recordMapper, never()).selectById(any());
        verify(recordMapper, never()).selectOne(any());
    }

    @Test
    void assignDevice_rejectsEveryNonIdlePrinterState() {
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        for (int state = 1; state <= 6; state++) {
            DeviceEntity device = readyDevice(2L);
            device.setState(state);
            when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.assignDevice(1L, dto), "state " + state);

            assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
        }
        verify(usageChecker, times(6)).isInUse(2L);
        verify(recordMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void assignDevice_rejectsIdleDeviceWithActiveProductionUsage() {
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(readyDevice(2L));
        when(usageChecker.isInUse(2L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.assignDevice(1L, dto));

        assertThat(exception.getCode()).isEqualTo(ErrorCodeEnum.DEVICE_NOT_AVAILABLE.getCode());
        verify(availabilityService).requireAvailable(any(DeviceEntity.class), eq(true));
        verify(recordMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void assignDevice_usageCheckFailureDoesNotContinue() {
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(readyDevice(2L));
        when(usageChecker.isInUse(2L)).thenThrow(new IllegalStateException("usage query failed"));

        assertThrows(IllegalStateException.class, () -> service.assignDevice(1L, dto));

        verifyNoInteractions(availabilityService);
        verify(recordMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void assignDevice_rejectsIncompleteProductWeights() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = readyDevice(2L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        dto.setProductWeights(List.of(weight(11L, "12.35")));
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        when(productMapper.selectList(any())).thenReturn(List.of(product(11L), product(12L)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> runAsAdmin(() -> service.assignDevice(1L, dto)));

        assertThat(exception.getMessage()).contains("重量");
        verifyNoInteractions(processMapper, productNumberService, deviceUsageCounterService);
    }

    @Test
    void assignDevice_rejectsDuplicateProductWeight() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> assignWithProducts(
                        List.of(weight(11L, "1.00"), weight(11L, "2.00")),
                        List.of(product(11L), product(12L))));

        assertThat(exception.getMessage()).contains("重复");
    }

    @Test
    void assignDevice_rejectsForeignOrNegativeProductWeight() {
        BusinessException foreignException = assertThrows(BusinessException.class,
                () -> assignWithProducts(
                        List.of(weight(999L, "1.00")),
                        List.of(product(11L))));
        assertThat(foreignException.getMessage()).contains("当前流转卡");

        BusinessException negativeException = assertThrows(BusinessException.class,
                () -> assignWithProducts(
                        List.of(weight(11L, "-0.01")),
                        List.of(product(11L))));
        assertThat(negativeException.getMessage()).contains("不能小于0");
    }

    @Test
    void assignDevice_rejectsProductionWorkerDeviceFromAnotherCenter() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = readyDevice(2L);
        device.setCenterId(200L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        dto.setProductWeights(List.of(weight(11L, "1.00")));
        UserEntity worker = new UserEntity();
        worker.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.PRODUCTION_WORKER.getCode());
        worker.setCenterId(100L);
        OrderMainEntity order = new OrderMainEntity();
        order.setId(10L);
        order.setCenterId(100L);
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        when(userMapper.selectById(7L)).thenReturn(worker);
        when(orderMainMapper.selectById(10L)).thenReturn(order);

        BusinessException exception;
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            exception = assertThrows(BusinessException.class, () -> service.assignDevice(1L, dto));
        }

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.FORBIDDEN.getCode());
        verifyNoInteractions(productMapper);
        verify(processMapper, never()).update(isNull(), any());
        verifyNoInteractions(deviceUsageCounterService, productNumberService);
    }

    @Test
    void assignDevice_rejectsProductionRecordFromAnotherCenter() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = readyDevice(2L);
        device.setCenterId(100L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        UserEntity worker = new UserEntity();
        worker.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.PRODUCTION_WORKER.getCode());
        worker.setCenterId(100L);
        OrderMainEntity order = new OrderMainEntity();
        order.setId(10L);
        order.setCenterId(200L);
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        when(userMapper.selectById(7L)).thenReturn(worker);
        when(orderMainMapper.selectById(10L)).thenReturn(order);

        BusinessException exception;
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            exception = assertThrows(BusinessException.class, () -> service.assignDevice(1L, dto));
        }

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.FORBIDDEN.getCode());
        verifyNoInteractions(productMapper, processMapper, deviceUsageCounterService, productNumberService);
    }

    @Test
    void assignDevice_rejectsNonProductionRole() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = readyDevice(2L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        UserEntity finance = new UserEntity();
        finance.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.FINANCE.getCode());
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        when(userMapper.selectById(7L)).thenReturn(finance);

        BusinessException exception;
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            exception = assertThrows(BusinessException.class, () -> service.assignDevice(1L, dto));
        }

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.FORBIDDEN.getCode());
        verifyNoInteractions(productMapper, processMapper, deviceUsageCounterService, productNumberService);
    }

    @Test
    void assignDevice_whenPrintProcessMissing_rollsBackBeforeNumberGeneration() {
        ProductionRecordEntity record = pendingRecord(1L);
        DeviceEntity device = readyDevice(2L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        dto.setProductWeights(List.of(weight(11L, "1.00")));
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(device);
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(productMapper.selectList(any())).thenReturn(List.of(product(11L)));
        when(productMapper.updateById(any(ProductionProductEntity.class))).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(0);
        doReturn(true).when(service).updateById(any(ProductionRecordEntity.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> runAsAdmin(() -> service.assignDevice(1L, dto)));

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.RECORD_STATUS_ABNORMAL.getCode());
        verifyNoInteractions(deviceUsageCounterService, productNumberService);
    }

    @Test
    void assignDevice_rejectsRecordThatAlreadyHasDevice() {
        ProductionRecordEntity record = pendingRecord(1L);
        record.setPrintDeviceId(2L);
        when(deviceMapper.selectByIdForUpdate(3L)).thenReturn(readyDevice(3L));
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);

        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(3L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> runAsAdmin(() -> service.assignDevice(1L, dto)));

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.RECORD_DEVICE_ALREADY_ASSIGNED.getCode());
        verify(deviceMapper).selectByIdForUpdate(3L);
        verify(usageChecker).isInUse(3L);
        verify(availabilityService).requireAvailable(any(DeviceEntity.class), eq(false));
        verifyNoInteractions(processMapper, productNumberService, deviceUsageCounterService);
    }

    @Test
    void releaseDevice_pendingRecordClearsEntirePrintConfiguration() {
        ProductionRecordEntity record = pendingRecord(1L);
        record.setPrintDeviceId(2L);
        record.setPrintDeviceCode("SLA-002");
        record.setPrintDeviceName("打印机2");
        record.setMaterial("树脂");
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);
        when(productMapper.update(isNull(), any())).thenReturn(2);

        runAsAdmin(() -> service.releaseDevice(1L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<ProductionRecordEntity>> recordUpdateCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(recordMapper).update(isNull(), recordUpdateCaptor.capture());
        String recordSql = normalizedSql(recordUpdateCaptor.getValue().getSqlSet());
        assertThat(recordSql).contains("printdeviceid", "printdevicecode", "printdevicename", "material", "contentupdatetime");
        assertThat(recordUpdateCaptor.getValue().getParamNameValuePairs().values())
                .anyMatch(LocalDateTime.class::isInstance);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<ProductionProcessEntity>> processUpdateCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(processMapper).update(isNull(), processUpdateCaptor.capture());
        String processSql = normalizedSql(processUpdateCaptor.getValue().getSqlSet());
        assertThat(processSql).contains("deviceid", "deviceno", "devicename", "processparams", "operatorid", "operatorname");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<ProductionProductEntity>> productUpdateCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(productMapper).update(isNull(), productUpdateCaptor.capture());
        String productSql = normalizedSql(productUpdateCaptor.getValue().getSqlSet());
        assertThat(productSql).contains("productno", "weight");
        assertThat(normalizedSql(productUpdateCaptor.getValue().getSqlSegment())).contains("productionrecordid");
        assertThat(productUpdateCaptor.getValue().getParamNameValuePairs()).containsValue(1L);

        verifyNoInteractions(deviceUsageCounterService, productNumberService);
    }

    @Test
    void releaseDevice_rejectsRecordOutsidePendingPrint() {
        ProductionRecordEntity record = pendingRecord(1L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> runAsAdmin(() -> service.releaseDevice(1L)));

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_RELEASE_DEVICE.getCode());
        verify(recordMapper, never()).update(isNull(), any());
        verifyNoInteractions(processMapper, productMapper, deviceUsageCounterService, productNumberService);
    }

    @Test
    void releaseDevice_alreadyReleasedPendingRecordIsIdempotent() {
        ProductionRecordEntity record = pendingRecord(1L);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        runAsAdmin(() -> service.releaseDevice(1L));

        verify(recordMapper).update(isNull(), any());
        verify(processMapper).update(isNull(), any());
        verify(productMapper).update(isNull(), any());
    }

    @Test
    void releaseDevice_rejectsProductionWorkerRecordFromAnotherCenter() {
        ProductionRecordEntity record = pendingRecord(1L);
        record.setPrintDeviceId(2L);
        UserEntity worker = new UserEntity();
        worker.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.PRODUCTION_WORKER.getCode());
        worker.setCenterId(100L);
        OrderMainEntity order = new OrderMainEntity();
        order.setId(10L);
        order.setCenterId(200L);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(7L)).thenReturn(worker);
        when(orderMainMapper.selectById(10L)).thenReturn(order);

        BusinessException exception;
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            exception = assertThrows(BusinessException.class, () -> service.releaseDevice(1L));
        }

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.FORBIDDEN.getCode());
        verify(recordMapper, never()).update(isNull(), any());
        verifyNoInteractions(processMapper, productMapper);
    }

    @Test
    void releaseDevice_rejectsNonProductionRole() {
        ProductionRecordEntity record = pendingRecord(1L);
        UserEntity finance = new UserEntity();
        finance.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.FINANCE.getCode());
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(7L)).thenReturn(finance);

        BusinessException exception;
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            exception = assertThrows(BusinessException.class, () -> service.releaseDevice(1L));
        }

        assertThat(exception.getCode()).isEqualTo(
                com.yigongbao.common.enums.ErrorCodeEnum.FORBIDDEN.getCode());
        verify(recordMapper, never()).update(isNull(), any());
        verifyNoInteractions(processMapper, productMapper);
    }

    private void assignWithProducts(List<AssignProductWeightDTO> weights,
                                    List<ProductionProductEntity> products) {
        ProductionRecordEntity record = pendingRecord(1L);
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(2L);
        dto.setProductWeights(weights);
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(deviceMapper.selectByIdForUpdate(2L)).thenReturn(readyDevice(2L));
        when(productMapper.selectList(any())).thenReturn(products);
        runAsAdmin(() -> service.assignDevice(1L, dto));
    }

    private DeviceEntity readyDevice(Long id) {
        DeviceEntity device = new DeviceEntity();
        device.setId(id);
        device.setDeviceId("SLA-" + id);
        device.setDeviceName("打印机" + id);
        device.setState(0);
        device.setConnectionStatus(1);
        return device;
    }

    private ProductionProductEntity product(Long id) {
        ProductionProductEntity product = new ProductionProductEntity();
        product.setId(id);
        product.setProductionRecordId(1L);
        return product;
    }

    private AssignProductWeightDTO weight(Long productId, String value) {
        AssignProductWeightDTO dto = new AssignProductWeightDTO();
        dto.setProductId(productId);
        dto.setWeight(value == null ? null : new BigDecimal(value));
        return dto;
    }

    private ProductionRecordEntity pendingRecord(Long id) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(id);
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        record.setOrderId(10L);
        return record;
    }

    private String normalizedSql(String sql) {
        return sql.toLowerCase().replace("_", "");
    }

    private void runAsAdmin(Runnable action) {
        UserEntity admin = new UserEntity();
        admin.setRoleCode(com.yigongbao.common.enums.RoleCodeEnum.ADMIN.getCode());
        when(userMapper.selectById(7L)).thenReturn(admin);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            action.run();
        }
    }
}
