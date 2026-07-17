package com.yigongbao.module.production.pack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import com.yigongbao.module.production.pack.service.IProductionPackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionPackController.class)
class ProductionPackControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionPackService packService;

    @Test
    void fillPackInfo_rejectsMissingDevice() throws Exception {
        mockMvc.perform(put("/production/pack/{id}/fill", 7L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(packService);
    }

    @Test
    void fillPackInfo_delegatesRequest() throws Exception {
        FillPackDTO dto = new FillPackDTO();
        dto.setPrimaryDeviceId(8L);
        mockMvc.perform(put("/production/pack/{id}/fill", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(packService).fillPackInfo(7L, dto);
    }

    @Test
    void transferToWarehouse_delegatesId() throws Exception {
        mockMvc.perform(post("/production/pack/{id}/transfer", 7L))
                .andExpect(status().isOk());
        verify(packService).transferToWarehouse(7L);
    }
}
