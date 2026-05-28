package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
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
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

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

    @InjectMocks
    private ProductionRecordServiceImpl recordService;

    @BeforeEach
    void setUp() throws Exception {
        Field f = ServiceImpl.class.getDeclaredField("baseMapper");
        f.setAccessible(true);
        f.set(recordService, recordMapper);
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
        ProductionProductEntity product = new ProductionProductEntity();
        product.setId(10L);
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        ProductionRecordVO vo = recordService.getRecordDetail(1L);

        assertNotNull(vo);
        assertEquals("REC-001", vo.getRecordNo());
        assertEquals(1, vo.getProducts().size());
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
        when(recordMapper.selectOne(any())).thenReturn(record);
        when(recordMapper.selectOne(any(), anyBoolean())).thenReturn(record);
        when(recordMapper.selectById(5L)).thenReturn(record);
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals("REC-005", recordService.getByRecordNo("REC-005").getRecordNo());
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
        when(designPackageMapper.selectById(1L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.downloadDataPackage(1L)).getCode());
    }

    @Test
    void downloadDataPackage_orderNotFound_throwsException() {
        when(designPackageMapper.selectById(1L)).thenReturn(pkg(1L, 10L));
        when(orderMainMapper.selectById(10L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> recordService.downloadDataPackage(1L)).getCode());
    }

    @Test
    void downloadDataPackage_startPrintNotAvailable_idempotentSkip() {
        when(designPackageMapper.selectById(1L)).thenReturn(pkg(1L, 10L));
        when(orderMainMapper.selectById(10L)).thenReturn(order(10L, ProductionConstants.ORDER_TYPE_MEDICAL));
        when(flowFacade.getAvailableActions(10L)).thenReturn(List.of("OTHER_ACTION"));

        recordService.downloadDataPackage(1L);

        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }

    @Test
    void downloadDataPackage_startPrintAvailable_triggersFlow() {
        when(designPackageMapper.selectById(1L)).thenReturn(pkg(1L, 10L));
        when(orderMainMapper.selectById(10L)).thenReturn(order(10L, ProductionConstants.ORDER_TYPE_MEDICAL));
        when(flowFacade.getAvailableActions(10L)).thenReturn(List.of(FlowActionEnum.START_PRINT.name()));
        TransitionResult result = buildResult();
        when(flowFacade.executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class))).thenReturn(result);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            recordService.downloadDataPackage(1L);
        }

        verify(flowFacade).executeFlow(eq(10L), eq(FlowActionEnum.START_PRINT), any(FlowOperator.class));
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
