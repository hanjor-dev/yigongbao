package com.yigongbao.module.production.process.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionProcessServiceImplTest {

    @Mock private ProductionProcessMapper processMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private IProductionRecordService recordService;
    @Mock private DeviceMapper deviceMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private ProductionProcessServiceImpl processService;

    @BeforeEach
    void setUp() throws Exception {
        Field f = ServiceImpl.class.getDeclaredField("baseMapper");
        f.setAccessible(true);
        f.set(processService, processMapper);
        initTableInfo(ProductionRecordEntity.class);
    }

    // ---- startProcess ----

    @Test
    void startProcess_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.startProcess(99L, startDto("wash", 2L))).getCode());
    }

    @Test
    void startProcess_processNotFound_throwsException() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.startProcess(1L, startDto("wash", 2L))).getCode());
    }

    @Test
    void startProcess_notPending_throwsException() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);
        assertThrows(BusinessException.class, () -> processService.startProcess(1L, startDto("wash", 2L)));
    }

    @Test
    void startProcess_print_isRejectedBecauseDeviceEventsOwnPrintingLifecycle() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        assertThrows(BusinessException.class, () -> processService.startProcess(1L, startDto("print", 2L)));
    }

    @Test
    void startProcess_cureBeforeWashCompleted_isRejected() {
        ProductionRecordEntity rec = rec(1L, 10L);
        rec.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
        rec.setPrintFinishTime(LocalDateTime.of(2026, 7, 19, 19, 33, 42));
        when(recordMapper.selectById(1L)).thenReturn(rec);
        ProductionProcessEntity cure = proc(2L, 1L, "cure");
        ProductionProcessEntity wash = proc(1L, 1L, "wash");
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(cure);
        when(processMapper.selectList(any())).thenReturn(List.of(wash, cure));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThrows(BusinessException.class, () -> processService.startProcess(1L, startDto("cure", 2L)));
        }
    }

    @Test
    void startProcess_postProcess_updatesRecordStatus() {
        ProductionRecordEntity rec = rec(1L, 10L);
        rec.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
        rec.setPrintFinishTime(LocalDateTime.of(2026, 7, 19, 19, 33, 42));
        when(recordMapper.selectById(1L)).thenReturn(rec);
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.PENDING.getCode());
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);
        when(processMapper.selectList(any())).thenReturn(List.of(p));
        when(deviceMapper.selectById(2L)).thenReturn(null);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(null);
            processService.startProcess(1L, startDto("wash", 2L));
        }
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.POST_PROCESSING.getValue().equals(((ProductionRecordEntity) r).getStatus())
                        && "wash".equals(((ProductionRecordEntity) r).getCurrentProcess())));
    }

    @Test
    void startProcess_postProcess_preservesFixedScheduleTime() {
        ProductionRecordEntity rec = rec(1L, 10L);
        rec.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
        rec.setPrintFinishTime(LocalDateTime.of(2026, 7, 19, 19, 33, 42));
        when(recordMapper.selectById(1L)).thenReturn(rec);
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        LocalDateTime expectedStart = LocalDateTime.of(2026, 7, 19, 19, 35, 42);
        p.setStartTime(expectedStart);
        p.setEndTime(expectedStart.plusMinutes(10));
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);
        when(deviceMapper.selectById(2L)).thenReturn(null);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(null);
            processService.startProcess(1L, startDto("wash", 2L));
        }

        assertEquals(expectedStart, p.getStartTime());
        assertEquals(expectedStart.plusMinutes(10), p.getEndTime());
    }

    @Test
    void startProcess_postProcess_withoutPrintFinishTime_throwsException() {
        ProductionRecordEntity rec = rec(1L, 10L);
        rec.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
        when(recordMapper.selectById(1L)).thenReturn(rec);
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);

        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.startProcess(1L, startDto("wash", 2L))).getCode());
        assertEquals(ProcessStatusEnum.PENDING.getCode(), p.getStatus());
        verify(processMapper, never()).updateById(any(ProductionProcessEntity.class));
    }

    // ---- finishProcess ----

    @Test
    void finishProcess_processNotFound_throwsException() {
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.finishProcess(1L, "wash")).getCode());
    }

    @Test
    void finishProcess_notInProgress_throwsException() {
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.PENDING.getCode());
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);
        assertThrows(BusinessException.class, () -> processService.finishProcess(1L, "wash"));
    }

    @Test
    void finishProcess_print_isRejectedBecauseDeviceEventsOwnPrintingLifecycle() {
        assertThrows(BusinessException.class, () -> processService.finishProcess(1L, "print"));
    }

    @Test
    void finishProcess_wash_advancesToCure() {
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        LocalDateTime expectedStart = LocalDateTime.of(2026, 7, 19, 19, 35, 42);
        p.setStartTime(expectedStart);
        p.setDeviceId(2L);
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);
        DeviceEntity device = new DeviceEntity();
        device.setProcessingMinutes(1);
        when(deviceMapper.selectById(2L)).thenReturn(device);
        ProductionRecordEntity record = rec(1L, 10L);
        record.setPrintFinishTime(LocalDateTime.of(2026, 7, 19, 19, 33, 42));
        when(recordMapper.selectById(1L)).thenReturn(record);
        processService.finishProcess(1L, "wash");
        assertEquals(expectedStart.plusMinutes(10), p.getEndTime());
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                ProcessTypeEnum.CURE.getCode().equals(((ProductionRecordEntity) r).getCurrentProcess())));
    }

    @Test
    void finishProcess_cleanDry_setsQcAndTriggersFlow() {
        ProductionProcessEntity p = proc(1L, 1L, "clean_dry");
        p.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        p.setStartTime(LocalDateTime.now());
        when(processMapper.selectOne(any(), anyBoolean())).thenReturn(p);
        ProductionRecordEntity rec = rec(1L, 10L);
        rec.setPrintFinishTime(LocalDateTime.of(2026, 7, 19, 19, 33, 42));
        when(recordMapper.selectById(1L)).thenReturn(rec);
        when(processMapper.selectList(any())).thenReturn(List.of(p));
        when(productMapper.selectCount(any())).thenReturn(1L);
        processService.finishProcess(1L, "clean_dry");
        verify(recordMapper).update(isNull(), any());
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.QC_IN_PROGRESS.getValue(), FlowActionEnum.COMPLETE_POST_PROCESSING);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    @Test
    void schedulePostProcessing_setsFixedTimesFromPrintFinishTime() {
        ProductionProcessEntity wash = proc(1L, 1L, "wash");
        ProductionProcessEntity cure = proc(2L, 1L, "cure");
        ProductionProcessEntity cleanDry = proc(3L, 1L, "clean_dry");
        ProductionProcessEntity pack = proc(4L, 1L, "pack");
        when(processMapper.selectList(any())).thenReturn(List.of(wash, cure, cleanDry, pack));

        processService.schedulePostProcessing(1L,
                LocalDateTime.of(2026, 7, 19, 19, 33, 42, 123_000_000));
        processService.schedulePostProcessing(1L,
                LocalDateTime.of(2026, 7, 19, 19, 33, 42, 999_000_000));

        assertEquals(LocalDateTime.of(2026, 7, 19, 19, 35, 42), wash.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 19, 19, 45, 42), wash.getEndTime());
        assertEquals(LocalDateTime.of(2026, 7, 19, 19, 46, 42), cure.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 19, 20, 26, 42), cure.getEndTime());
        assertEquals(LocalDateTime.of(2026, 7, 19, 20, 27, 42), cleanDry.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 19, 20, 37, 42), cleanDry.getEndTime());
        verify(processMapper, times(6)).updateById(any(ProductionProcessEntity.class));
        verify(processMapper, never()).updateById(org.mockito.ArgumentMatchers.<ProductionProcessEntity>argThat(process ->
                ProcessTypeEnum.PACK.getCode().equals(process.getProcessType())));
    }

    @Test
    void schedulePostProcessing_withoutPrintFinishTime_throwsException() {
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.schedulePostProcessing(1L, null)).getCode());
        verify(processMapper, never()).selectList(any());
    }

    @Test
    void schedulePostProcessing_duplicateProcess_throwsException() {
        ProductionProcessEntity wash1 = proc(1L, 1L, "wash");
        ProductionProcessEntity wash2 = proc(2L, 1L, "wash");
        when(processMapper.selectList(any())).thenReturn(List.of(wash1, wash2));

        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.schedulePostProcessing(1L,
                                LocalDateTime.of(2026, 7, 19, 19, 33, 42))).getCode());
        verify(processMapper, never()).updateById(any(ProductionProcessEntity.class));
    }

    // ---- helpers ----

    private ProductionProcessEntity proc(Long id, Long recordId, String type) {
        ProductionProcessEntity p = new ProductionProcessEntity();
        p.setId(id);
        p.setProductionRecordId(recordId);
        p.setProcessType(type);
        p.setStatus(ProcessStatusEnum.PENDING.getCode());
        return p;
    }

    private ProductionRecordEntity rec(Long id, Long orderId) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(orderId);
        r.setRecordNo("REC-00" + id);
        return r;
    }

    private StartProcessDTO startDto(String processType, Long deviceId) {
        StartProcessDTO dto = new StartProcessDTO();
        dto.setProcessType(processType);
        dto.setPrimaryDeviceId(deviceId);
        return dto;
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }
}
