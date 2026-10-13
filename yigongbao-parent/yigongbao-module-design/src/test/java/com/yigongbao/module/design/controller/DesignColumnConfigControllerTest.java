package com.yigongbao.module.design.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
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

@WebMvcTest(DesignColumnConfigController.class)
class DesignColumnConfigControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DesignWorkorderService designWorkorderService;

    @Test
    void getColumnConfig_delegatesService() throws Exception {
        mockMvc.perform(get("/design/column-config"))
                .andExpect(status().isOk());
        verify(designWorkorderService).getColumnConfig();
    }

    @Test
    void saveColumnConfig_rejectsNullColumns() throws Exception {
        mockMvc.perform(post("/design/column-config")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(designWorkorderService);
    }

    @Test
    void saveColumnConfig_delegatesValidColumns() throws Exception {
        mockMvc.perform(post("/design/column-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columns\":[{\"field\":\"orderCode\",\"label\":\"订单号\",\"visible\":true,\"sort\":1}]}"))
                .andExpect(status().isOk());
        verify(designWorkorderService).saveColumnConfig(org.mockito.ArgumentMatchers.any());
    }
}
