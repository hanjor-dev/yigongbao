package com.yigongbao.module.production.record.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.record.dto.SaveProductionColumnConfigDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionColumnConfigController.class)
class ProductionColumnConfigControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionRecordService service;

    @Test
    void getColumnConfig_delegatesService() throws Exception {
        mockMvc.perform(get("/production/column-config"))
                .andExpect(status().isOk());
        verify(service).getColumnConfig();
    }

    @Test
    void saveColumnConfig_rejectsMissingColumns() throws Exception {
        mockMvc.perform(post("/production/column-config")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void saveColumnConfig_delegatesValidColumns() throws Exception {
        mockMvc.perform(post("/production/column-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columns\":[{\"field\":\"recordNo\",\"label\":\"流转卡号\",\"visible\":true,\"sort\":1}]}"))
                .andExpect(status().isOk());
        verify(service).saveColumnConfig(org.mockito.ArgumentMatchers.any());
    }
}
