package com.yigongbao.module.basic.device.controller;

import com.yigongbao.module.basic.device.service.IDeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@ContextConfiguration(classes = DeviceController.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IDeviceService deviceService;

    @Test
    void updateState_acceptsPrinterStateSixAndDelegatesToService() throws Exception {
        mockMvc.perform(put("/basic/device/42/state").param("state", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deviceService).updateDeviceState(42L, 6);
    }

    @Test
    void updateState_rejectsStateSevenWithoutCallingService() throws Exception {
        mockMvc.perform(put("/basic/device/42/state").param("state", "7"))
                .andExpect(status().isBadRequest());

        verify(deviceService, never()).updateDeviceState(42L, 7);
    }

    @Test
    void list_rejectsPageSizeAboveMaximumWithoutCallingService() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/basic/device/list")
                        .contentType("application/json")
                        .content("{\"pageNum\":1,\"pageSize\":101}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(deviceService);
    }

    @Test
    void list_rejectsInvalidConnectionStatusWithoutCallingService() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/basic/device/list")
                        .contentType("application/json")
                        .content("{\"pageNum\":1,\"pageSize\":10,\"connectionStatus\":2}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(deviceService);
    }
}
