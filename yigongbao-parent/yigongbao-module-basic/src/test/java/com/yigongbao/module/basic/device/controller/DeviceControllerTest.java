package com.yigongbao.module.basic.device.controller;

import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.vo.DeviceStatisticsVO;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void statistics_returnsDeviceStatistics() throws Exception {
        DeviceStatisticsVO expected = new DeviceStatisticsVO();
        expected.setTotal(3L);
        expected.setIdle(1L);
        expected.setOccupied(2L);
        when(deviceService.getStatistics(org.mockito.ArgumentMatchers.any(DevicePageDTO.class)))
                .thenReturn(expected);

        mockMvc.perform(get("/basic/device/statistics")
                        .param("centerId", "7")
                        .param("deviceType", "PRINTER_SLA")
                        .param("state", "1")
                        .param("connectionStatus", "1")
                        .param("deviceId", "SLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.idle").value(1))
                .andExpect(jsonPath("$.data.occupied").value(2));

        ArgumentCaptor<DevicePageDTO> dtoCaptor = ArgumentCaptor.forClass(DevicePageDTO.class);
        verify(deviceService).getStatistics(dtoCaptor.capture());
        DevicePageDTO actual = dtoCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(7L, actual.getCenterId());
        org.junit.jupiter.api.Assertions.assertEquals("PRINTER_SLA", actual.getDeviceType());
        org.junit.jupiter.api.Assertions.assertEquals(1, actual.getState());
        org.junit.jupiter.api.Assertions.assertEquals(1, actual.getConnectionStatus());
        org.junit.jupiter.api.Assertions.assertEquals("SLA", actual.getDeviceId());
    }

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
