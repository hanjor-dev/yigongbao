package com.yigongbao.module.basic.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.service.impl.DeviceServiceImpl;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.lang.reflect.Field;
import java.util.Arrays;

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

        deviceService.batchUpdateDeviceStatus(dto);

        verify(deviceMapper, times(1)).insert(any(DeviceEntity.class));
    }
}
