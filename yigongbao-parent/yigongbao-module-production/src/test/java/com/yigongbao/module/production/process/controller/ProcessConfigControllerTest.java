package com.yigongbao.module.production.process.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcessConfigController.class)
class ProcessConfigControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @MockBean private ConfigService configService;
    @MockBean private DeviceMapper deviceMapper;
    @MockBean private UserMapper userMapper;

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
        when(deviceMapper.selectList(any())).thenReturn(java.util.List.of());
        try (org.mockito.MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            mockMvc.perform(get("/production/process-config/devices")
                            .param("deviceType", "PRINTER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
        verify(deviceMapper).selectList(any());
    }
}
