package com.yigongbao.module.design.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.design.dto.SavePrintInfoDTO;
import com.yigongbao.module.design.service.DesignPrintInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignPrintInfoController.class)
class DesignPrintInfoControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DesignPrintInfoService printInfoService;

    @Test
    void savePrintInfo_rejectsMissingProductMarkAndItems() throws Exception {
        mockMvc.perform(post("/design/workorder/1/package/2/print-info")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(printInfoService);
    }

    @Test
    void savePrintInfo_allowsEmptyItemsToClearPackage() throws Exception {
        SavePrintInfoDTO dto = new SavePrintInfoDTO();
        dto.setProductMark("PM-1");
        dto.setItems(java.util.List.of());
        mockMvc.perform(post("/design/workorder/1/package/2/print-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(printInfoService).savePrintInfo(eq(1L), eq(2L), any(SavePrintInfoDTO.class));
    }

    @Test
    void deletePrintInfo_delegatesAllIds() throws Exception {
        mockMvc.perform(delete("/design/workorder/1/package/2/print-info/3"))
                .andExpect(status().isOk());
        verify(printInfoService).deletePrintInfo(1L, 2L, 3L);
    }

    @Test
    void optionsAndList_delegateOrderAndPackageIds() throws Exception {
        mockMvc.perform(get("/design/workorder/1/package/2/print-info/options"))
                .andExpect(status().isOk());
        verify(printInfoService).getOptions(1L, 2L);

        mockMvc.perform(get("/design/workorder/1/package/2/print-info"))
                .andExpect(status().isOk());
        verify(printInfoService).listPrintInfo(1L, 2L);
    }
}
