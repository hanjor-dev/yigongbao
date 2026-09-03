package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

@ExtendWith(MockitoExtension.class)
class ProductionPrintLifecycleServiceImplTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private ProductionProcessMapper processMapper;
    @Mock private IProductionProcessService processService;
    @Mock private IProductionRecordService recordService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private ProductionPrintLifecycleServiceImpl service;

    @BeforeEach
    void initMybatisLambdaCache() {
        if (TableInfoHelper.getTableInfo(ProductionRecordEntity.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), ""), ProductionRecordEntity.class);
        }
        if (TableInfoHelper.getTableInfo(ProductionProcessEntity.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), ""), ProductionProcessEntity.class);
        }
    }

    @Test
    void forceCompletePrint_updatesRecordProcessAndAdvancesOrder() {
        ProductionRecordEntity record = printingRecord();
        UserEntity user = productionManager();
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(9L)).thenReturn(user);
        when(recordMapper.update(isNull(), any())).thenReturn(1);
        when(processMapper.update(isNull(), any())).thenReturn(1);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);
            service.forceCompletePrint(1L);
        }

        verify(processService).schedulePostProcessing(eq(1L), any(LocalDateTime.class));
        verify(recordService).triggerFlowIfAllReach(eq(20L), eq(FlowStatusEnum.PRINT_COMPLETED.getValue()), any());
        verify(recordService).reconcileOrderProductionStatus(20L);
    }

    @Test
    void forceCompletePrint_rejectsNonProductionManager() {
        ProductionRecordEntity record = printingRecord();
        UserEntity user = productionManager();
        user.setRoleCode(RoleCodeEnum.PRODUCTION_WORKER.getCode());
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(9L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);
            assertThrows(BusinessException.class, () -> service.forceCompletePrint(1L));
        }

        verifyNoInteractions(processService, processMapper, recordService);
    }

    @Test
    void forceCompletePrint_rejectsManagerFromAnotherCenter() {
        ProductionRecordEntity record = printingRecord();
        UserEntity user = productionManager();
        user.setCenterId(31L);
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(9L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);
            assertThrows(BusinessException.class, () -> service.forceCompletePrint(1L));
        }

        verifyNoInteractions(processService, processMapper, recordService);
    }

    @Test
    void forceCompletePrint_rejectsRecordNotInPrintingState() {
        ProductionRecordEntity record = printingRecord();
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        UserEntity user = productionManager();
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(9L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);
            assertThrows(BusinessException.class, () -> service.forceCompletePrint(1L));
        }

        verifyNoInteractions(processService, processMapper, recordService);
    }

    @Test
    void forceCompletePrint_isIdempotentForCompletedRecord() {
        ProductionRecordEntity record = printingRecord();
        record.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
        UserEntity user = productionManager();
        when(recordMapper.selectByIdForUpdate(1L)).thenReturn(record);
        when(userMapper.selectById(9L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);
            assertDoesNotThrow(() -> service.forceCompletePrint(1L));
        }

        verifyNoInteractions(processService, processMapper, recordService);
    }

    private ProductionRecordEntity printingRecord() {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(1L);
        record.setOrderId(20L);
        record.setProcessingCenterId(30L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        record.setPrintFinishTime(LocalDateTime.of(2026, 9, 3, 18, 0));
        return record;
    }

    private UserEntity productionManager() {
        UserEntity user = new UserEntity();
        user.setId(9L);
        user.setRoleCode(RoleCodeEnum.PRODUCTION_MANAGER.getCode());
        user.setCenterId(30L);
        return user;
    }
}
