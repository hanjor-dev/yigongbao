package com.yigongbao.module.production.pack.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.junit.jupiter.api.Test;
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
    @Mock private IProductionRecordService recordService;

    @InjectMocks
    private ProductionPackServiceImpl packService;

    @Test
    void fillPackInfo_recordNotFound_throwsException() {
        when(recordMapper.selectById(99L)).thenReturn(null);
        assertEquals(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND.getCode(),
                assertThrows(BusinessException.class, () -> packService.fillPackInfo(99L, new FillPackDTO())).getCode());
    }

    @Test
    void fillPackInfo_deviceNotFound_throwsException() {
        FillPackDTO dto = new FillPackDTO();
        dto.setPackDeviceId(5L);
        when(recordMapper.selectById(1L)).thenReturn(record(1L));
        when(deviceMapper.selectById(5L)).thenReturn(null);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertEquals(ErrorCodeEnum.PACK_DEVICE_NOT_FOUND.getCode(),
                    assertThrows(BusinessException.class, () -> packService.fillPackInfo(1L, dto)).getCode());
        }
    }

    @Test
    void fillPackInfo_success_updatesPackFields() {
        FillPackDTO dto = new FillPackDTO();
        dto.setPackDeviceId(5L);
        dto.setPackSterilizationBatchNo("BATCH-S-001");
        DeviceEntity device = new DeviceEntity();
        device.setId(5L);
        device.setDeviceId("PACK-DEV-001");
        when(recordMapper.selectById(1L)).thenReturn(record(1L));
        when(deviceMapper.selectById(5L)).thenReturn(device);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            packService.fillPackInfo(1L, dto);
        }

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(r -> {
            ProductionRecordEntity e = (ProductionRecordEntity) r;
            return Long.valueOf(5L).equals(e.getPackDeviceId())
                    && "PACK-DEV-001".equals(e.getPackDeviceNo())
                    && "BATCH-S-001".equals(e.getPackSterilizationBatchNo())
                    && Long.valueOf(1L).equals(e.getPackOperatorId())
                    && e.getPackTime() != null;
        }));
    }

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
    void transferToWarehouse_success_updatesStatusAndTriggersFlow() {
        ProductionRecordEntity r = record(1L);
        r.setPackDeviceId(5L);
        r.setOrderId(10L);
        when(recordMapper.selectById(1L)).thenReturn(r);

        packService.transferToWarehouse(1L);

        verify(recordMapper).updateById((ProductionRecordEntity) argThat(rec ->
                FlowStatusEnum.WAREHOUSE_IN.getValue().equals(((ProductionRecordEntity) rec).getStatus())));
        verify(recordService).triggerFlowIfAllReach(10L,
                FlowStatusEnum.WAREHOUSE_IN.getValue(), FlowActionEnum.COMPLETE_WAREHOUSE_IN);
    }

    private ProductionRecordEntity record(Long id) {
        ProductionRecordEntity r = new ProductionRecordEntity();
        r.setId(id);
        r.setOrderId(10L);
        r.setRecordNo("REC-00" + id);
        return r;
    }
}
