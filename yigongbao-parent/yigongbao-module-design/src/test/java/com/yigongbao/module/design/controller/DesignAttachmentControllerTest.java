package com.yigongbao.module.design.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.design.dto.LinkFilesDTO;
import com.yigongbao.module.design.service.DesignFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignAttachmentController.class)
class DesignAttachmentControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DesignFileService designFileService;

    @Test
    void linkModels_rejectsEmptyFileIds() throws Exception {
        LinkFilesDTO dto = new LinkFilesDTO();
        dto.setOrderId(1L);
        dto.setFileIds(java.util.List.of());
        mockMvc.perform(post("/design/models/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(designFileService);
    }

    @Test
    void linkModels_delegatesOrderAndFiles() throws Exception {
        LinkFilesDTO dto = new LinkFilesDTO();
        dto.setOrderId(1L);
        dto.setFileIds(java.util.List.of("f1", "f2"));
        mockMvc.perform(post("/design/models/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(designFileService).linkModels(eq(1L), eq(java.util.List.of("f1", "f2")));
    }

    @Test
    void deleteReport_delegatesOrderAndFileId() throws Exception {
        mockMvc.perform(delete("/design/report/{fileId}", "f1").param("orderId", "1"))
                .andExpect(status().isOk());
        verify(designFileService).deleteReport(1L, "f1");
    }

    @Test
    void modelQueriesAndDelete_delegateOrderAndModelIds() throws Exception {
        mockMvc.perform(get("/design/models").param("orderId", "1"))
                .andExpect(status().isOk());
        verify(designFileService).listModels(1L);

        mockMvc.perform(delete("/design/model/{modelId}", 2L).param("orderId", "1"))
                .andExpect(status().isOk());
        verify(designFileService).deleteModel(1L, 2L);
    }

    @Test
    void reportLinkAndQuery_delegateFileService() throws Exception {
        LinkFilesDTO dto = new LinkFilesDTO();
        dto.setOrderId(1L);
        dto.setFileIds(java.util.List.of("report-1"));
        mockMvc.perform(post("/design/report/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(designFileService).linkReport(1L, "report-1");

        mockMvc.perform(get("/design/report").param("orderId", "1"))
                .andExpect(status().isOk());
        verify(designFileService).getReport(1L);
    }
}
