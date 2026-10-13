package com.yigongbao.module.production.qc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.qc.dto.SaveQcColumnConfigDTO;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QcColumnConfigController.class)
class QcColumnConfigControllerTest {

    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionQcService qcService;

    @Test
    void getColumnConfig_delegatesService() throws Exception {
        mockMvc.perform(get("/production/qc/column-config"))
                .andExpect(status().isOk());
        verify(qcService).getColumnConfig();
    }

    @Test
    void saveColumnConfig_delegatesValidRequest() throws Exception {
        SaveQcColumnConfigDTO dto = new SaveQcColumnConfigDTO();
        SaveQcColumnConfigDTO.ColumnItemDTO item = new SaveQcColumnConfigDTO.ColumnItemDTO();
        item.setField("recordNo");
        item.setLabel("流转卡编号");
        item.setVisible(true);
        item.setSort(1);
        dto.setColumns(java.util.List.of(item));
        mockMvc.perform(post("/production/qc/column-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(qcService).saveColumnConfig(any(SaveQcColumnConfigDTO.class));
    }

    @Test
    void saveColumnConfig_rejectsInvalidNestedColumn() throws Exception {
        mockMvc.perform(post("/production/qc/column-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"columns\":[{\"field\":\"\",\"label\":\"\",\"visible\":null,\"sort\":null}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(qcService);
    }
}
