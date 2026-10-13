package com.yigongbao.module.production.pack.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionPackServiceImplTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProductionProcessMapper processMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private IProductionRecordService recordService;

    @InjectMocks
    private ProductionPackServiceImpl packService;

    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setRealName("张三");
        initTableInfo(ProductionProcessEntity.class);
        initTableInfo(ProductionProductEntity.class);
        initTableInfo(ProductionRecordEntity.class);
        // 预先打桩，避免 LambdaUpdateWrapper 触发 MyBatis Plus lambda 缓存解析
        when(processMapper.update(any(), any())).thenReturn(1);
        when(productMapper.update(any(), any())).thenReturn(1);
    }

    // ==================== fillPackInfo ====================

    @Test
    void fillPackInfo_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        FillPackDTO dto = new FillPackDTO();

        BusinessException ex = assertThrows(BusinessException.class,
            () -> packService.fillPackInfo(99L, dto));
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void fillPackInfo_recordNotInPackingStatus_throwsException() {
        ProductionRecordEntity record = record(1L);
        record.setStatus(FlowStatusEnum.PRINTING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);

        FillPackDTO dto = new FillPackDTO();
        BusinessException ex = assertThrows(BusinessException.class,
            () -> packService.fillPackInfo(1L, dto));
        assertEquals(ErrorCodeEnum.RECORD_NOT_IN_PACKING_STATUS.getCode(), ex.getCode());
    }

    @Test
    void fillPackInfo_deviceNotFound_throwsException() {
        ProductionRecordEntity record = record(1L);
        record.setStatus(FlowStatusEnum.PACKING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);

        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(99L);
        when(deviceMapper.selectById(99L)).thenReturn(null);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                () -> packService.fillPackInfo(1L, dto));
            assertEquals(ErrorCodeEnum.PACK_DEVICE_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Test
    void fillPackInfo_nonSealingDevice_isRejected() {
        ProductionRecordEntity record = record(1L);
        when(recordMapper.selectById(1L)).thenReturn(record);
        DeviceEntity device = new DeviceEntity();
        device.setId(5L);
        device.setDeviceType(DeviceTypeEnum.PRINTER_SLA.getCode());
        when(deviceMapper.selectById(5L)).thenReturn(device);
        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(5L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThrows(BusinessException.class, () -> packService.fillPackInfo(1L, dto));
        }
    }

    @Test
    void fillPackInfo_missingPackProcess_isRejected() {
        ProductionRecordEntity record = record(1L);
        when(recordMapper.selectById(1L)).thenReturn(record);
        DeviceEntity device = new DeviceEntity();
        device.setId(5L);
        device.setDeviceType(DeviceTypeEnum.SEALING_MACHINE.getCode());
        when(deviceMapper.selectById(5L)).thenReturn(device);
        when(processMapper.update(any(), any())).thenReturn(0);
        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(5L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThrows(BusinessException.class, () -> packService.fillPackInfo(1L, dto));
        }
    }

    @Test
    void fillPackInfo_withProcessParams_success() {
        ProductionRecordEntity record = record(1L);
        record.setStatus(FlowStatusEnum.PACKING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);

        DeviceEntity device = new DeviceEntity();
        device.setId(5L);
        device.setDeviceId("PACK-001");
        device.setDeviceName("包装机A");
        device.setDeviceType(DeviceTypeEnum.SEALING_MACHINE.getCode());
        when(deviceMapper.selectById(5L)).thenReturn(device);
        when(userMapper.selectById(1L)).thenReturn(mockUser);

        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(5L);
        dto.setProcessParams("{\"sealTemperature\":\"180\",\"sealTime\":30,\"sterilizationMethod\":\"EO\",\"sterilizationBatchNo\":\"BATCH-001\"}");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            packService.fillPackInfo(1L, dto);
        }

        verify(recordMapper).updateById(argThat((ProductionRecordEntity r) -> {
            ProductionRecordEntity e = (ProductionRecordEntity) r;
            return Long.valueOf(5L).equals(e.getPackDeviceId())
                && "PACK-001".equals(e.getPackDeviceNo())
                && e.getPackSealTemperature() != null
                && Integer.valueOf(30).equals(e.getPackSealTime())
                && "EO".equals(e.getPackSterilizationMethod())
                && "BATCH-001".equals(e.getPackSterilizationBatchNo())
                && e.getPackTime() != null;
        }));
        verify(processMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void fillPackInfo_withSecondaryDevice_success() {
        ProductionRecordEntity record = record(1L);
        record.setStatus(FlowStatusEnum.PACKING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);

        DeviceEntity primary = new DeviceEntity();
        primary.setId(5L);
        primary.setDeviceId("PACK-001");
        primary.setDeviceName("包装机");
        primary.setDeviceType(DeviceTypeEnum.SEALING_MACHINE.getCode());

        DeviceEntity secondary = new DeviceEntity();
        secondary.setId(6L);
        secondary.setDeviceId("STERILE-001");
        secondary.setDeviceName("灭菌设备");

        when(deviceMapper.selectById(5L)).thenReturn(primary);
        when(deviceMapper.selectById(6L)).thenReturn(secondary);
        when(userMapper.selectById(1L)).thenReturn(mockUser);

        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(5L);
        dto.setSecondaryDeviceId(6L);
        dto.setProcessParams("{}");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            packService.fillPackInfo(1L, dto);
        }

        verify(processMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void fillPackInfo_emptyProcessParams_success() {
        ProductionRecordEntity record = record(1L);
        record.setStatus(FlowStatusEnum.PACKING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(record);

        DeviceEntity device = new DeviceEntity();
        device.setId(5L);
        device.setDeviceId("PACK-001");
        device.setDeviceType(DeviceTypeEnum.SEALING_MACHINE.getCode());
        when(deviceMapper.selectById(5L)).thenReturn(device);
        when(userMapper.selectById(1L)).thenReturn(mockUser);

        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(5L);
        dto.setProcessParams("");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertDoesNotThrow(() -> packService.fillPackInfo(1L, dto));
        }
    }

    // ==================== transferToWarehouse ====================

    @Test
    void transferToWarehouse_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> packService.transferToWarehouse(99L)).getCode());
    }

    @Test
    void transferToWarehouse_packDeviceIdNull_throwsException() {
        ProductionRecordEntity r = record(1L);
        r.setPackDeviceId(null);
        when(recordMapper.selectById(1L)).thenReturn(r);
        assertEquals(ErrorCodeEnum.PACK_INFO_NOT_FILLED.getCode(),
                assertThrows(BusinessException.class, () -> packService.transferToWarehouse(1L)).getCode());
    }

    @Test
    void transferToWarehouse_recordNotInPackingStatus_throwsException() {
        ProductionRecordEntity r = record(1L);
        r.setPackDeviceId(5L);
        r.setStatus(FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue()); // 已经流转过
        when(recordMapper.selectById(1L)).thenReturn(r);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> packService.transferToWarehouse(1L));
        assertEquals(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_TRANSFER_TO_PACK.getCode(), ex.getCode());
    }

    @Test
    void transferToWarehouse_success_updatesStatusAndTriggersFlow() {
        ProductionRecordEntity r = record(1L);
        r.setPackDeviceId(5L);
        r.setOrderId(10L);
        r.setStatus(FlowStatusEnum.PACKING.getValue());
        when(recordMapper.selectById(1L)).thenReturn(r);

        packService.transferToWarehouse(1L);

        verify(recordMapper).updateById(argThat((ProductionRecordEntity rec) ->
            FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue().equals(((ProductionRecordEntity) rec).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
            FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue(), FlowActionEnum.COMPLETE_PACKING);
        verify(recordService).reconcileOrderProductionStatus(10L);
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }

    private ProductionRecordEntity record(Long id) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(10L);
        r.setRecordNo("REC-00" + id);
        r.setStatus(FlowStatusEnum.PACKING.getValue());
        return r;
    }
}
