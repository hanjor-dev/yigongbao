package com.yigongbao.module.production.process.service.impl;

import cn.dev33.satoken.stp.StpUtil;
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

import java.lang.reflect.Field;
import java.time.LocalDateTime;

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
        when(processMapper.selectOne(any())).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.startProcess(1L, startDto("wash", 2L))).getCode());
    }

    @Test
    void startProcess_notPending_throwsException() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        when(processMapper.selectOne(any())).thenReturn(p);
        assertThrows(BusinessException.class, () -> processService.startProcess(1L, startDto("wash", 2L)));
    }

    @Test
    void startProcess_postProcess_updatesRecordStatus() {
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.PENDING.getCode());
        when(processMapper.selectOne(any())).thenReturn(p);
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

    // ---- finishProcess ----

    @Test
    void finishProcess_processNotFound_throwsException() {
        when(processMapper.selectOne(any())).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class,
                        () -> processService.finishProcess(1L, "wash")).getCode());
    }

    @Test
    void finishProcess_notInProgress_throwsException() {
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.PENDING.getCode());
        when(processMapper.selectOne(any())).thenReturn(p);
        assertThrows(BusinessException.class, () -> processService.finishProcess(1L, "wash"));
    }

    @Test
    void finishProcess_wash_advancesToCure() {
        ProductionProcessEntity p = proc(1L, 1L, "wash");
        p.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        p.setStartTime(LocalDateTime.now());
        when(processMapper.selectOne(any())).thenReturn(p);
        when(recordMapper.selectById(1L)).thenReturn(rec(1L, 10L));
        processService.finishProcess(1L, "wash");
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                ProcessTypeEnum.CURE.getCode().equals(((ProductionRecordEntity) r).getCurrentProcess())));
    }

    @Test
    void finishProcess_cleanDry_setsQcAndTriggersFlow() {
        ProductionProcessEntity p = proc(1L, 1L, "clean_dry");
        p.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        p.setStartTime(LocalDateTime.now());
        when(processMapper.selectOne(any())).thenReturn(p);
        ProductionRecordEntity rec = rec(1L, 10L);
        when(recordMapper.selectById(1L)).thenReturn(rec);
        processService.finishProcess(1L, "clean_dry");
        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r ->
                FlowStatusEnum.QC_IN_PROGRESS.getValue().equals(((ProductionRecordEntity) r).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.QC_IN_PROGRESS.getValue(), FlowActionEnum.COMPLETE_POST_PROCESSING);
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
}
