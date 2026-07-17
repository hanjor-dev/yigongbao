package com.yigongbao.module.production.process.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionProcessController.class)
class ProductionProcessControllerTest {

    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionProcessService processService;

    @Test
    void startProcess_rejectsMissingRequiredDevice() throws Exception {
        mockMvc.perform(post("/production/process/{id}/start", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processType\":\"wash\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(processService);
    }

    @Test
    void startProcess_delegatesDto() throws Exception {
        StartProcessDTO dto = new StartProcessDTO();
        dto.setProcessType("wash");
        dto.setPrimaryDeviceId(8L);

        mockMvc.perform(post("/production/process/{id}/start", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(processService).startProcess(eq(7L), any(StartProcessDTO.class));
    }

    @Test
    void finishProcess_requiresProcessType() throws Exception {
        mockMvc.perform(post("/production/process/{id}/finish", 7L))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(processService);
    }

    @Test
    void listAndFinish_delegateRecordIdAndProcessType() throws Exception {
        mockMvc.perform(get("/production/process/{id}/list", 7L))
                .andExpect(status().isOk());
        verify(processService).listProcesses(7L);

        mockMvc.perform(post("/production/process/{id}/finish", 7L)
                        .param("processType", "wash"))
                .andExpect(status().isOk());
        verify(processService).finishProcess(7L, "wash");
    }
}
