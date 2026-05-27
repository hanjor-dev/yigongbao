package com.yigongbao.module.production.process.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.transfer.entity.ProductionProcessTransferEntity;
import com.yigongbao.module.production.transfer.mapper.ProductionProcessTransferMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionProcessServiceImplTest {

    @Mock private ProductionProcessMapper processMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionProcessTransferMapper transferMapper;
    @Mock private IProductionRecordService recordService;

    @InjectMocks
    private ProductionProcessServiceImpl processService;

    @BeforeEach
    void setUp() throws Exception {
        Field f = ServiceImpl.class.getDeclaredField("baseMapper");
        f.setAccessible(true);
        f.set(processService, processMapper);
    }

    // ---- fillProcess ----

    @Test
    void fillProcess_processNotFound_throwsException() {
        when(processMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> processService.fillProcess(99L, new FillProcessDTO())).getCode());
    }

    @Test
    void fillProcess_noRedoProducts_doesNotUpdateRecord() {
        when(processMapper.selectById(1L)).thenReturn(proc(1L, 10L, ProcessTypeEnum.PRINT.getCode()));
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        processService.fillProcess(1L, fillDto(5L));

        verify(processMapper).updateById((ProductionProcessEntity) argThat(p ->
                ProcessStatusEnum.COMPLETED.getCode().equals(((ProductionProcessEntity) p).getStatus())));
        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
    }

    @Test
    void fillProcess_hasRedoProducts_restoresThemAndClearsFlag() {
        when(processMapper.selectById(1L)).thenReturn(proc(1L, 10L, ProcessTypeEnum.PRINT.getCode()));
        when(productMapper.selectList(any())).thenReturn(List.of(new ProductionProductEntity()));
        when(productMapper.selectCount(any())).thenReturn(0L);

        processService.fillProcess(1L, fillDto(5L));

        verify(productMapper).updateById((ProductionProductEntity) argThat(p ->
                ProductStatusEnum.IN_PROCESS.getCode().equals(((ProductionProductEntity) p).getStatus())
                        && ((ProductionProductEntity) p).getRedoProcessType() == null));
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                Integer.valueOf(0).equals(((ProductionRecordEntity) r).getHasRedoProduct())));
    }

    @Test
    void fillProcess_hasRedoProducts_remainRedoExists_doesNotClearFlag() {
        when(processMapper.selectById(1L)).thenReturn(proc(1L, 10L, ProcessTypeEnum.PRINT.getCode()));
        when(productMapper.selectList(any())).thenReturn(List.of(new ProductionProductEntity()));
        when(productMapper.selectCount(any())).thenReturn(1L);

        processService.fillProcess(1L, fillDto(5L));

        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
    }

    // ---- transferToNext ----

    @Test
    void transferToNext_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                    assertThrows(BusinessException.class,
                            () -> processService.transferToNext(99L, ProcessTypeEnum.PRINT.getCode(), null)).getCode());
        }
    }

    @Test
    void transferToNext_fromPrint_setsPrintCompletedAndTriggersFlow() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            processService.transferToNext(1L, ProcessTypeEnum.PRINT.getCode(), ProcessTypeEnum.WASH.getCode());
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.PRINT_COMPLETED.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L, FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
    }

    @Test
    void transferToNext_fromWash_setsPostProcessing() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            processService.transferToNext(1L, ProcessTypeEnum.WASH.getCode(), ProcessTypeEnum.CURE.getCode());
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r -> {
            ProductionRecordEntity e = (ProductionRecordEntity) r;
            return RecordStatusEnum.POST_PROCESSING.getCode().equals(e.getStatus())
                    && ProcessTypeEnum.CURE.getCode().equals(e.getCurrentProcess());
        }));
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    @Test
    void transferToNext_fromCure_setsPostProcessing() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            processService.transferToNext(1L, ProcessTypeEnum.CURE.getCode(), ProcessTypeEnum.CLEAN_DRY.getCode());
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                RecordStatusEnum.POST_PROCESSING.getCode().equals(((ProductionRecordEntity) r).getStatus())));
    }

    @Test
    void transferToNext_fromCleanDry_setsQcInProgressAndTriggersFlow() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            processService.transferToNext(1L, ProcessTypeEnum.CLEAN_DRY.getCode(), ProcessTypeEnum.PACK.getCode());
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.QC_IN_PROGRESS.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L, FlowStatusEnum.QC_IN_PROGRESS.getValue(), FlowActionEnum.COMPLETE_POST_PROCESSING);
    }

    @Test
    void transferToNext_fromOtherProcess_doesNotUpdateStatus() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            mockStp(stp);
            processService.transferToNext(1L, ProcessTypeEnum.PACK.getCode(), null);
        }
        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
        verify(recordService, never()).triggerFlowIfAllReach(any(), any(), any());
    }

    // ---- handlePrintFailure ----

    @Test
    void handlePrintFailure_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> processService.handlePrintFailure(99L, "r", false));
    }

    @Test
    void handlePrintFailure_recreateFalse_returnsNullWithoutUpdate() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        assertNull(processService.handlePrintFailure(1L, "r", false));
        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
    }

    @Test
    void handlePrintFailure_recreateTrue_setsPrintFailedAndDeletesProducts() {
        ProductionProductEntity p = new ProductionProductEntity();
        p.setId(100L);
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        when(productMapper.selectList(any())).thenReturn(List.of(p));

        assertNull(processService.handlePrintFailure(1L, "r", true));

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                RecordStatusEnum.PRINT_FAILED.getCode().equals(((ProductionRecordEntity) r).getStatus())));
        verify(productMapper).deleteById(100L);
    }

    // ---- handlePrintInspectionFail ----

    @Test
    void handlePrintInspectionFail_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> processService.handlePrintInspectionFail(99L, "r", false));
    }

    @Test
    void handlePrintInspectionFail_recreateFalse_returnsNull() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        assertNull(processService.handlePrintInspectionFail(1L, "r", false));
        verify(recordMapper, never()).updateById((ProductionRecordEntity) any());
    }

    @Test
    void handlePrintInspectionFail_recreateTrue_setsAbandonedAndDeletesProducts() {
        ProductionProductEntity p = new ProductionProductEntity();
        p.setId(200L);
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        when(productMapper.selectList(any())).thenReturn(List.of(p));

        assertNull(processService.handlePrintInspectionFail(1L, "r", true));

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                RecordStatusEnum.ABANDONED.getCode().equals(((ProductionRecordEntity) r).getStatus())));
        verify(productMapper).deleteById(200L);
    }

    // ---- helpers ----

    private ProductionProcessEntity proc(Long id, Long recordId, String type) {
        ProductionProcessEntity p = new ProductionProcessEntity();
        p.setId(id);
        p.setProductionRecordId(recordId);
        p.setProcessType(type);
        return p;
    }

    private ProductionRecordEntity rec(Long id, Long orderId) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(orderId);
        r.setRecordNo("REC-00" + id);
        return r;
    }

    private FillProcessDTO fillDto(Long deviceId) {
        FillProcessDTO dto = new FillProcessDTO();
        dto.setDeviceId(deviceId);
        return dto;
    }

    private void mockStp(MockedStatic<StpUtil> stp) {
        stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        SaSession session = mock(SaSession.class);
        when(session.get(anyString(), anyString())).thenReturn("user");
        stp.when(StpUtil::getSession).thenReturn(session);
    }
}
