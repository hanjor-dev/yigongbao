package com.yigongbao.module.production.process.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.record.service.PrinterAvailabilityService;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessConfigController.class)
@Import(PrinterAvailabilityService.class)
class ProcessConfigControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @MockBean private ConfigService configService;
    @MockBean private DeviceMapper deviceMapper;
    @MockBean private UserMapper userMapper;
    @MockBean private PrinterDeviceUsageChecker usageChecker;

    @Test
    void steps_returnsConfiguredProcessDefinitions() throws Exception {
        mockMvc.perform(get("/production/process-config/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void params_parsesConfiguredJson() throws Exception {
        when(configService.getConfigValue(any())).thenReturn("{\"wash\":[]}");

        mockMvc.perform(get("/production/process-config/params"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wash").isArray());
    }

    @Test
    void devices_filtersByRequestedTypeAndReturnsGroups() throws Exception {
        UserEntity user = new UserEntity();
        when(userMapper.selectById(1L)).thenReturn(user);
        List<DeviceEntity> devices = List.of(
                device(1L, 1, 0),
                device(2L, 1, 0),
                device(3L, 1, 5),
                device(4L, 0, 0));
        when(deviceMapper.selectList(any())).thenReturn(devices);
        when(usageChecker.findActiveDeviceIds(List.of(1L, 2L, 3L, 4L))).thenReturn(Set.of(2L));
        try (org.mockito.MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            mockMvc.perform(get("/production/process-config/devices")
                            .param("deviceType", DeviceTypeEnum.PRINTER_SLA.getCode()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].devices[0].status").value(0))
                    .andExpect(jsonPath("$.data[0].devices[0].statusName").value("空闲"))
                    .andExpect(jsonPath("$.data[0].devices[0].available").value(true))
                    .andExpect(jsonPath("$.data[0].devices[0].deviceState").value(0))
                    .andExpect(jsonPath("$.data[0].devices[0].deviceStateName").value("空闲"))
                    .andExpect(jsonPath("$.data[0].devices[0].connectionStatus").value(1))
                    .andExpect(jsonPath("$.data[0].devices[1].status").value(1))
                    .andExpect(jsonPath("$.data[0].devices[1].statusName").value("占用"))
                    .andExpect(jsonPath("$.data[0].devices[1].available").value(false))
                    .andExpect(jsonPath("$.data[0].devices[2].deviceStateName").value("准备就绪"))
                    .andExpect(jsonPath("$.data[0].devices[2].available").value(false))
                    .andExpect(jsonPath("$.data[0].devices[3].available").value(false));
        }
        verify(deviceMapper).selectList(any());
        verify(usageChecker).findActiveDeviceIds(List.of(1L, 2L, 3L, 4L));
    }

    @Test
    void devices_nonPrinterPreservesBinaryStateWithoutPrinterUsageLookup() throws Exception {
        UserEntity user = new UserEntity();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(deviceMapper.selectList(any())).thenReturn(List.of(device(5L, 1, 1)));

        try (org.mockito.MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            mockMvc.perform(get("/production/process-config/devices")
                            .param("deviceType", DeviceTypeEnum.WASH_CONTAINER.getCode()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].devices[0].status").value(1))
                    .andExpect(jsonPath("$.data[0].devices[0].statusName").value("占用"))
                    .andExpect(jsonPath("$.data[0].devices[0].deviceState").value(1))
                    .andExpect(jsonPath("$.data[0].devices[0].deviceStateName").value("占用"))
                    .andExpect(jsonPath("$.data[0].devices[0].connectionStatus").value(1))
                    .andExpect(jsonPath("$.data[0].devices[0].available").value(false));
        }

        verify(deviceMapper).selectList(any());
        verifyNoInteractions(usageChecker);
    }

    private DeviceEntity device(Long id, Integer connectionStatus, Integer state) {
        DeviceEntity device = new DeviceEntity();
        device.setId(id);
        device.setDeviceId("SLA-" + id);
        device.setDeviceName("打印机" + id);
        device.setCenterId(10L);
        device.setCenterName("加工中心");
        device.setConnectionStatus(connectionStatus);
        device.setState(state);
        return device;
    }
}
