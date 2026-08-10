package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SaveProductionColumnConfigDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.vo.ProductionColumnConfigVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionRecordServiceImplTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignPackageFileMapper designPackageFileMapper;
    @Mock private OrderMainMapper orderMainMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionProcessMapper processMapper;
    @Mock private FlowFacade flowFacade;
    @Mock private UserMapper userMapper;
    @Mock private ConfigService configService;
    @Mock private UserService userService;
    @Mock private ObjectMapper objectMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FlowCardExcelBuilder flowCardExcelBuilder;
    @Mock private com.yigongbao.module.basic.file.service.FileService fileService;

    @InjectMocks
    private ProductionRecordServiceImpl recordService;

    @BeforeEach
    void setUp() throws Exception {
        Field f = ServiceImpl.class.getDeclaredField("baseMapper");
        f.setAccessible(true);
        f.set(recordService, recordMapper);

        if (TableInfoHelper.getTableInfo(ProductionRecordEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, ProductionRecordEntity.class);
        }
        if (TableInfoHelper.getTableInfo(OrderMainEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, OrderMainEntity.class);
        }
        if (TableInfoHelper.getTableInfo(ProductionProcessEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, ProductionProcessEntity.class);
        }
    }

    // ---- getRecordDetail ----

    @Test
    void getRecordDetail_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.getRecordDetail(99L)).getCode());
    }

    @Test
    void getRecordDetail_found_returnsVoWithProducts() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(1L);
        record.setRecordNo("REC-001");
        record.setFlowCardFileUrl("/flow-card.xlsx");
        record.setFlowCardGenerateTime(java.time.LocalDateTime.now());
        record.setContentUpdateTime(record.getFlowCardGenerateTime());
        ProductionProductEntity product = new ProductionProductEntity();
        product.setId(10L);
        product.setWeight(new BigDecimal("12.35"));
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        ProductionRecordVO vo = recordService.getRecordDetail(1L);

        assertNotNull(vo);
        assertEquals("REC-001", vo.getRecordNo());
        assertEquals(1, vo.getProducts().size());
        assertEquals(new BigDecimal("12.35"), vo.getProducts().get(0).getWeight());
    }

    // ---- pageRecords ----

    @Test
    void pageRecords_mapsProductionEndTimeToPrintFinishTimeForListCompatibility() {
        LocalDateTime printFinishTime = LocalDateTime.of(2026, 8, 10, 10, 30);
        LocalDateTime postProcessingEndTime = LocalDateTime.of(2026, 8, 10, 11, 45);
        ProductionRecordEntity record = record(1L, 10L, FlowStatusEnum.PRINT_COMPLETED.getValue());
        record.setPrintFinishTime(printFinishTime);
        record.setPostProcessingEndTime(postProcessingEndTime);

        Page<ProductionRecordEntity> entityPage = new Page<>(1, 10);
        entityPage.setRecords(List.of(record));
        entityPage.setTotal(1);
        when(recordMapper.selectPage(any(Page.class), any())).thenReturn(entityPage);
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(orderMainMapper.selectList(any())).thenReturn(List.of(order(10L, ProductionConstants.ORDER_TYPE_MEDICAL)));

        ProductionRecordPageDTO dto = new ProductionRecordPageDTO();
        dto.setProcessingCenterId(20L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            IPage<ProductionRecordVO> result = recordService.pageRecords(dto);

            assertEquals(printFinishTime, result.getRecords().get(0).getPrintFinishTime());
            assertEquals(printFinishTime, result.getRecords().get(0).getPostProcessingEndTime());
        }
    }

    @Test
    void generateFlowCardExcel_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(processMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.generateFlowCardExcel(99L)).getCode());
    }

    @Test
    void getOrGenerateFlowCardExcel_reusesFreshCachedFileWithPatientPrefix() {
        ProductionRecordEntity record = record(100L, 200L, FlowStatusEnum.PRINT_COMPLETED.getValue());
        record.setFlowCardFileUrl("https://files/card.xlsx");
        record.setFlowCardGenerateTime(java.time.LocalDateTime.now().minusMinutes(5));
        record.setContentUpdateTime(java.time.LocalDateTime.now().minusMinutes(10));
        OrderMainEntity order = order(200L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setPatientName("患者甲");
        when(recordMapper.selectById(100L)).thenReturn(record);
        when(orderMainMapper.selectById(200L)).thenReturn(order);

        var file = recordService.getOrGenerateFlowCardExcel(100L);

        assertEquals("https://files/card.xlsx", file.getFileUrl());
        assertEquals("患者甲流转卡.xlsx", file.getFileName());
        verifyNoInteractions(flowCardExcelBuilder, fileService);
    }

    @Test
    void triggerFlowAndSync_nullTransitionDoesNotWriteOrder() {
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class)))
                .thenReturn(null);

        recordService.triggerFlowAndSync(10L, FlowActionEnum.COMPLETE_PRINT);

        verify(recordMapper, never()).updateById(any(ProductionRecordEntity.class));
    }

    @Test
    void triggerFlowAndSync_writesTransitionToOrder() {
        TransitionResult result = mock(TransitionResult.class);
        when(result.getTargetPhase()).thenReturn(30);
        when(result.getFinalStatus()).thenReturn(FlowStatusEnum.PRINT_COMPLETED.getValue());
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class)))
                .thenReturn(result);

        recordService.triggerFlowAndSync(10L, FlowActionEnum.COMPLETE_PRINT);

        verify(orderMainMapper).updateById(argThat((OrderMainEntity order) ->
                order.getId().equals(10L)
                        && order.getPhase().equals(30)
                        && order.getStatus().equals(FlowStatusEnum.PRINT_COMPLETED.getValue())));
    }

    @Test
    void triggerFlowAndSync_ignoresRejectedTransition() {
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class)))
                .thenThrow(new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR));

        recordService.triggerFlowAndSync(10L, FlowActionEnum.COMPLETE_PRINT);

        verify(orderMainMapper, never()).updateById(any(OrderMainEntity.class));
    }

    // ---- getByRecordNo ----

    @Test
    void getByRecordNo_notFound_throwsException() {
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.getByRecordNo("REC-X")).getCode());
    }

    @Test
    void getByRecordNo_found_delegatesToGetRecordDetail() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(5L);
        record.setRecordNo("REC-005");
        record.setFlowCardFileUrl("/flow-card.xlsx");
        record.setFlowCardGenerateTime(java.time.LocalDateTime.now());
        record.setContentUpdateTime(record.getFlowCardGenerateTime());
        when(recordMapper.selectOne(any())).thenReturn(record);
        when(recordMapper.selectOne(any(), anyBoolean())).thenReturn(record);
        when(recordMapper.selectById(5L)).thenReturn(record);
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals("REC-005", recordService.getByRecordNo("REC-005").getRecordNo());
    }

    @Test
    void generateBatchNo_existingRecord_returnsPreviewWithoutPersisting() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(20L);
        when(recordMapper.selectById(20L)).thenReturn(record);
        when(codeGeneratorService.generate(ProductionConstants.PRODUCTION_BATCH_NO)).thenReturn("260717");

        assertEquals("260717", recordService.generateBatchNo(20L));

        verify(codeGeneratorService).generate(ProductionConstants.PRODUCTION_BATCH_NO);
        verify(recordMapper, never()).updateById(org.mockito.ArgumentMatchers.<ProductionRecordEntity>any());
    }

    @Test
    void submitBatchNo_updatesProductionAndMaterialBatch() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(21L);
        when(recordMapper.selectById(21L)).thenReturn(record);
        doReturn(1).when(recordMapper).updateById(
                org.mockito.ArgumentMatchers.<ProductionRecordEntity>any());
        com.yigongbao.module.production.record.dto.SubmitBatchNoDTO dto =
                new com.yigongbao.module.production.record.dto.SubmitBatchNoDTO();
        dto.setProductionBatchNo("260717");
        dto.setMaterialBatchNo("MAT-01");

        recordService.submitBatchNo(21L, dto);

        var captor = org.mockito.ArgumentCaptor.forClass(ProductionRecordEntity.class);
        verify(recordMapper).updateById(captor.capture());
        assertEquals("260717", captor.getValue().getProductionBatchNo());
        assertEquals("MAT-01", captor.getValue().getMaterialBatchNo());
    }

    @Test
    void getDeviceConfig_existingRecord_copiesAssignedDeviceFields() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(22L);
        record.setMaterial("光敏树脂");
        record.setPrintDeviceId(3L);
        record.setPrintDeviceCode("SLA-003");
        record.setPrintDeviceName("打印机3");
        when(recordMapper.selectById(22L)).thenReturn(record);
        ProductionProcessEntity printProcess = new ProductionProcessEntity();
        printProcess.setProcessType("print");
        printProcess.setProcessParams("{\"layerHeight\":0.05}");
        when(processMapper.selectOne(any())).thenReturn(printProcess);

        var config = recordService.getDeviceConfig(22L);

        assertEquals(3L, config.getPrintDeviceId());
        assertEquals("SLA-003", config.getPrintDeviceCode());
        assertEquals("打印机3", config.getPrintDeviceName());
        assertEquals("光敏树脂", config.getMaterial());
        assertEquals("{\"layerHeight\":0.05}", config.getPrintParams());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ProductionProcessEntity>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(processMapper).selectOne(queryCaptor.capture());
        LambdaQueryWrapper<ProductionProcessEntity> query = queryCaptor.getValue();
        String sqlSegment = query.getSqlSegment().toLowerCase().replace("_", "");
        assertTrue(sqlSegment.contains("productionrecordid"));
        assertTrue(sqlSegment.contains("processtype"));
        assertFalse(sqlSegment.contains("limit"));
        assertTrue(query.getParamNameValuePairs().containsValue(22L));
        assertTrue(query.getParamNameValuePairs().containsValue("print"));
    }

    @Test
    void getDeviceConfig_withoutPrintProcess_returnsNullPrintParams() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(23L);
        record.setMaterial("尼龙");
        when(recordMapper.selectById(23L)).thenReturn(record);
        when(processMapper.selectOne(any())).thenReturn(null);

        var config = recordService.getDeviceConfig(23L);

        assertEquals("尼龙", config.getMaterial());
        assertNull(config.getPrintParams());
    }

    @Test
    void getDeviceConfig_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> recordService.getDeviceConfig(99L));

        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(), exception.getCode());
        verify(processMapper, never()).selectOne(any());
    }

    // ---- getQrCodeUrl ----

    @Test
    void getQrCodeUrl_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.getQrCodeUrl(99L)).getCode());
    }

    @Test
    void getQrCodeUrl_found_returnsUrl() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(1L);
        record.setQrCodeUrl("data:image/png;base64,abc");
        when(recordMapper.selectById(1L)).thenReturn(record);
        assertEquals("data:image/png;base64,abc", recordService.getQrCodeUrl(1L));
    }

    // ---- downloadDataPackage ----

    @Test
    void downloadDataPackage_designPackageNotFound_throwsException() {
        when(recordMapper.selectOne(any())).thenReturn(record(1L, 10L, FlowStatusEnum.DESIGN_COMPLETED.getValue()));
        when(designPackageMapper.selectById(1L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.downloadDataPackage(1L)).getCode());
    }

    @Test
    void downloadDataPackage_orderNotFound_throwsException() {
        ProductionRecordEntity record = record(1L, 10L, FlowStatusEnum.DESIGN_COMPLETED.getValue());
        record.setDesignPackageId(1L);
        when(recordMapper.selectOne(any())).thenReturn(record);
        when(designPackageMapper.selectById(1L)).thenReturn(pkg(1L, 10L));
        when(orderMainMapper.selectById(10L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.downloadDataPackage(1L)).getCode());
    }

    @Test
    void downloadDataPackage_startPrintNotAvailable_idempotentSkip() {
        ProductionRecordEntity record = record(1L, 10L, FlowStatusEnum.PENDING_PRINT.getValue());
        record.setDesignPackageId(1L);
        when(recordMapper.selectOne(any())).thenReturn(record);
        when(recordMapper.update(any(), any())).thenReturn(1);
        when(designPackageMapper.selectById(1L)).thenReturn(pkg(1L, 10L));
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(flowFacade.getAvailableActions(10L)).thenReturn(List.of("OTHER_ACTION"));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.downloadDataPackage(1L);
        }

        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }

    @Test
    void downloadDataPackage_startPrintAvailable_triggersFlow() {
        ProductionRecordEntity record = record(1L, 10L, FlowStatusEnum.PENDING_PRINT.getValue());
        record.setDesignPackageId(1L);
        when(recordMapper.selectOne(any())).thenReturn(record);
        when(recordMapper.update(any(), any())).thenReturn(1);
        when(designPackageMapper.selectById(1L)).thenReturn(pkg(1L, 10L));
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(flowFacade.getAvailableActions(10L)).thenReturn(List.of(FlowActionEnum.START_PRINT.name()));
        TransitionResult result = buildResult();
        when(recordMapper.selectCount(any())).thenReturn(1L).thenReturn(1L);
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.DOWNLOAD_DATA_PACKAGE), any(FlowOperator.class))).thenReturn(result);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.downloadDataPackage(1L);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.DOWNLOAD_DATA_PACKAGE), any(FlowOperator.class));
    }

    // ---- triggerFlowIfAllReach ----

    @Test
    void triggerFlowIfAllReach_totalActiveZero_doesNotTrigger() {
        when(recordMapper.selectCount(any())).thenReturn(0L);
        recordService.triggerFlowIfAllReach(10L, FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }

    @Test
    void triggerFlowIfAllReach_notAllReached_doesNotTrigger() {
        when(recordMapper.selectCount(any())).thenReturn(3L).thenReturn(2L);
        recordService.triggerFlowIfAllReach(10L, FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }

    @Test
    void triggerFlowIfAllReach_allReached_triggersFlow() {
        when(recordMapper.selectCount(any())).thenReturn(2L).thenReturn(2L);
        TransitionResult result = buildResult();
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class))).thenReturn(result);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.triggerFlowIfAllReach(10L, FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class));
    }

    // ---- reconcileOrderProductionStatus ----

    @Test
    void reconcileOrderProductionStatus_orderStillDesignCompleted_replaysDownloadAndPrintActions() {
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(recordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(2L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(3L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(4L, 10L, FlowStatusEnum.PRINT_COMPLETED.getValue())
        ));
        when(flowFacade.executeFlow(eq(10L), any(FlowActionEnum.class), any(FlowOperator.class)))
                .thenAnswer(invocation -> resultForAction(invocation.getArgument(1)));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.reconcileOrderProductionStatus(10L);
        }

        InOrder inOrder = inOrder(flowFacade);
        inOrder.verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.DOWNLOAD_DATA_PACKAGE), any(FlowOperator.class));
        inOrder.verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class));
        inOrder.verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class));
    }

    @Test
    void reconcileOrderProductionStatus_orderBehindPrintCompleted_replaysStartAndCompletePrint() {
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(recordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(2L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(3L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(4L, 10L, FlowStatusEnum.PRINT_COMPLETED.getValue())
        ));
        when(flowFacade.executeFlow(eq(10L), any(FlowActionEnum.class), any(FlowOperator.class)))
                .thenAnswer(invocation -> resultForAction(invocation.getArgument(1)));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.reconcileOrderProductionStatus(10L);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class));
        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class));
    }

    @Test
    void reconcileOrderProductionStatus_stopsWhenIntermediateFlowTransitionFails() {
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(recordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(2L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(3L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(4L, 10L, FlowStatusEnum.PRINT_COMPLETED.getValue())
        ));
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class)))
                .thenThrow(new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.reconcileOrderProductionStatus(10L);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class));
        verify(flowFacade, never()).executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class));
    }

    @Test
    void reconcileOrderProductionStatus_orderBehindPrinting_replaysStartPrintOnly() {
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(recordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(2L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(3L, 10L, FlowStatusEnum.QC_IN_PROGRESS.getValue()),
                record(4L, 10L, FlowStatusEnum.PRINTING.getValue())
        ));
        when(flowFacade.executeFlow(eq(10L), any(FlowActionEnum.class), any(FlowOperator.class)))
                .thenAnswer(invocation -> resultForAction(invocation.getArgument(1)));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.reconcileOrderProductionStatus(10L);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class));
        verify(flowFacade, never()).executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class));
    }

    @Test
    void reconcileOrderProductionStatus_ignoresInactiveRecordsWhenCalculatingMinimumStatus() {
        OrderMainEntity order = order(10L, ProductionConstants.ORDER_TYPE_MEDICAL);
        order.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(orderMainMapper.selectById(10L)).thenReturn(order);
        when(recordMapper.selectList(any())).thenReturn(List.of(
                record(1L, 10L, FlowStatusEnum.CANCELLED.getValue()),
                record(2L, 10L, FlowStatusEnum.PRINT_FAILED.getValue()),
                record(3L, 10L, FlowStatusEnum.REWORK.getValue()),
                record(4L, 10L, FlowStatusEnum.PRINT_COMPLETED.getValue())
        ));
        when(flowFacade.executeFlow(eq(10L), any(FlowActionEnum.class), any(FlowOperator.class)))
                .thenAnswer(invocation -> resultForAction(invocation.getArgument(1)));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.reconcileOrderProductionStatus(10L);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.COMPLETE_PRINT), any(FlowOperator.class));
    }

    // ---- getColumnConfig ----

    @Test
    void getColumnConfig_userHasConfig_returnsUserConfig() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setProductionColumnSettings("{\"columns\":[{\"field\":\"recordNo\",\"visible\":true}]}");

        ProductionColumnConfigVO expectedConfig = new ProductionColumnConfigVO();
        ProductionColumnConfigVO.ColumnItemVO item = new ProductionColumnConfigVO.ColumnItemVO();
        item.setField("recordNo");
        item.setVisible(true);
        expectedConfig.setColumns(List.of(item));

        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.readValue(anyString(), eq(ProductionColumnConfigVO.class))).thenReturn(expectedConfig);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            ProductionColumnConfigVO result = recordService.getColumnConfig();

            assertNotNull(result);
            assertEquals(1, result.getColumns().size());
            assertEquals("recordNo", result.getColumns().get(0).getField());
            verify(configService, never()).getConfigValue(any());
        }
    }

    @Test
    void getColumnConfig_userConfigInvalid_fallbackToSystemConfig() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setProductionColumnSettings("invalid json");

        ProductionColumnConfigVO systemConfig = new ProductionColumnConfigVO();
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.readValue("invalid json", ProductionColumnConfigVO.class))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("parse error") {});
        when(configService.getConfigValue(SystemConfigKeyEnum.PRODUCTION_COLUMN_CONFIG.getKey()))
            .thenReturn("{\"columns\":[]}");
        when(objectMapper.readValue("{\"columns\":[]}", ProductionColumnConfigVO.class)).thenReturn(systemConfig);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            ProductionColumnConfigVO result = recordService.getColumnConfig();

            assertNotNull(result);
            verify(configService).getConfigValue(SystemConfigKeyEnum.PRODUCTION_COLUMN_CONFIG.getKey());
        }
    }

    @Test
    void getColumnConfig_noUserConfig_returnsSystemConfig() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setProductionColumnSettings(null);

        ProductionColumnConfigVO systemConfig = new ProductionColumnConfigVO();
        when(userService.getById(1L)).thenReturn(user);
        when(configService.getConfigValue(SystemConfigKeyEnum.PRODUCTION_COLUMN_CONFIG.getKey()))
            .thenReturn("{\"columns\":[]}");
        when(objectMapper.readValue("{\"columns\":[]}", ProductionColumnConfigVO.class)).thenReturn(systemConfig);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            ProductionColumnConfigVO result = recordService.getColumnConfig();

            assertNotNull(result);
            verify(configService).getConfigValue(SystemConfigKeyEnum.PRODUCTION_COLUMN_CONFIG.getKey());
        }
    }

    @Test
    void getColumnConfig_systemConfigEmpty_returnsEmptyConfig() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);

        when(userService.getById(1L)).thenReturn(user);
        when(configService.getConfigValue(SystemConfigKeyEnum.PRODUCTION_COLUMN_CONFIG.getKey())).thenReturn("");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            ProductionColumnConfigVO result = recordService.getColumnConfig();

            assertNotNull(result);
            assertNull(result.getColumns());
        }
    }

    // ---- saveColumnConfig ----

    @Test
    void saveColumnConfig_userNotFound_throwsException() {
        when(userService.getById(1L)).thenReturn(null);

        SaveProductionColumnConfigDTO dto = new SaveProductionColumnConfigDTO();

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                () -> recordService.saveColumnConfig(dto));
            assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Test
    void saveColumnConfig_success_savesUserConfig() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);

        SaveProductionColumnConfigDTO dto = new SaveProductionColumnConfigDTO();
        SaveProductionColumnConfigDTO.ColumnItemDTO item = new SaveProductionColumnConfigDTO.ColumnItemDTO();
        item.setField("recordNo");
        item.setLabel("流转卡编号");
        item.setVisible(true);
        item.setSort(1);
        dto.setColumns(List.of(item));

        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"columns\":[]}");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            recordService.saveColumnConfig(dto);

            verify(userService).updateById(argThat(u -> {
                UserEntity entity = (UserEntity) u;
                return "{\"columns\":[]}".equals(entity.getProductionColumnSettings());
            }));
        }
    }

    @Test
    void saveColumnConfig_serializationFails_throwsException() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);

        SaveProductionColumnConfigDTO dto = new SaveProductionColumnConfigDTO();
        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.writeValueAsString(any()))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("error") {});

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                () -> recordService.saveColumnConfig(dto));
            assertEquals(ErrorCodeEnum.SYSTEM_ERROR.getCode(), ex.getCode());
        }
    }

    @Test
    void saveColumnConfig_emptyColumns_success() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);

        SaveProductionColumnConfigDTO dto = new SaveProductionColumnConfigDTO();
        dto.setColumns(Collections.emptyList());

        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"columns\":[]}");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertDoesNotThrow(() -> recordService.saveColumnConfig(dto));
            verify(userService).updateById(any());
        }
    }

    @Test
    void saveColumnConfig_nullColumns_success() throws Exception {
        UserEntity user = new UserEntity();
        user.setId(1L);

        SaveProductionColumnConfigDTO dto = new SaveProductionColumnConfigDTO();
        dto.setColumns(null);

        when(userService.getById(1L)).thenReturn(user);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"columns\":null}");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertDoesNotThrow(() -> recordService.saveColumnConfig(dto));
        }
    }

    // ---- helpers ----

    private DesignPackageEntity pkg(Long id, Long orderId) {
        DesignPackageEntity p = new DesignPackageEntity();
        p.setId(id);
        p.setOrderId(orderId);
        p.setPackageCode("PKG-001");
        return p;
    }

    private OrderMainEntity order(Long id, Integer orderType) {
        OrderMainEntity o = new OrderMainEntity();
        o.setId(id);
        o.setOrderCode("ORD-001");
        o.setOrderType(orderType);
        return o;
    }

    private ProductionRecordEntity record(Long id, Long orderId, Integer status) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(orderId);
        r.setStatus(status);
        return r;
    }

    private DeviceEntity device(Long id) {
        DeviceEntity d = new DeviceEntity();
        d.setId(id);
        d.setDeviceId("DEV-001");
        d.setDeviceName("打印机A");
        d.setCenterId(1L);
        d.setCenterName("加工中心A");
        return d;
    }

    private TransitionResult buildResult() {
        TransitionResult r = mock(TransitionResult.class);
        when(r.getTargetPhase()).thenReturn(1);
        when(r.getFinalStatus()).thenReturn(2);
        return r;
    }

    private TransitionResult resultForAction(FlowActionEnum action) {
        Map<FlowActionEnum, Integer> finalStatuses = Map.of(
                FlowActionEnum.DOWNLOAD_DATA_PACKAGE, FlowStatusEnum.PENDING_PRINT.getValue(),
                FlowActionEnum.START_PRINT, FlowStatusEnum.PRINTING.getValue(),
                FlowActionEnum.COMPLETE_PRINT, FlowStatusEnum.PRINT_COMPLETED.getValue(),
                FlowActionEnum.START_POST_PROCESSING, FlowStatusEnum.POST_PROCESSING.getValue(),
                FlowActionEnum.COMPLETE_POST_PROCESSING, FlowStatusEnum.QC_IN_PROGRESS.getValue(),
                FlowActionEnum.QC_PASS, FlowStatusEnum.PACKING.getValue(),
                FlowActionEnum.COMPLETE_PACKING, FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue(),
                FlowActionEnum.COMPLETE_WAREHOUSE_IN, FlowStatusEnum.WAREHOUSED.getValue(),
                FlowActionEnum.COMPLETE_WAREHOUSE_OUT, FlowStatusEnum.WAREHOUSE_OUT.getValue()
        );
        TransitionResult r = mock(TransitionResult.class);
        when(r.getTargetPhase()).thenReturn(1);
        when(r.getFinalStatus()).thenReturn(finalStatuses.get(action));
        return r;
    }

    private TransitionResult mockResult() {
        return buildResult();
    }

    private void mockStp(MockedStatic<StpUtil> stp) {
        stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        SaSession session = mock(SaSession.class);
        when(session.get("username")).thenReturn("testUser");
        stp.when(StpUtil::getSession).thenReturn(session);
    }
}
