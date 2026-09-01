package com.yigongbao.module.design.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.design.dto.CompleteDesignDTO;
import com.yigongbao.module.design.dto.StartDesignDTO;
import com.yigongbao.module.design.dto.UpdateEvaluationOpinionDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.order.service.OrderExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignWorkorderController.class)
class DesignWorkorderControllerTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DesignWorkorderService designWorkorderService;
    @MockBean private OrderExportService orderExportService;

    @Test
    void startDesign_rejectsMissingVersion() throws Exception {
        mockMvc.perform(post("/design/workorder/{id}/start-design", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(designWorkorderService);
    }

    @Test
    void startDesign_passesVersion() throws Exception {
        StartDesignDTO dto = new StartDesignDTO();
        dto.setVersion(2);

        mockMvc.perform(post("/design/workorder/{id}/start-design", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(designWorkorderService).startDesign(1L, 2);
    }

    @Test
    void completeDesign_passesVersion() throws Exception {
        CompleteDesignDTO dto = new CompleteDesignDTO();
        dto.setVersion(3);

        mockMvc.perform(post("/design/workorder/{id}/complete-design", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(designWorkorderService).completeDesign(1L, 3);
    }

    @Test
    void updateEvaluationOpinion_passesOrderIdAndOpinion() throws Exception {
        UpdateEvaluationOpinionDTO dto = new UpdateEvaluationOpinionDTO();
        dto.setDataEvaluationOpinion("影像数据清晰，可以进行设计");

        mockMvc.perform(post("/design/workorder/{id}/evaluation-opinion", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(designWorkorderService).updateEvaluationOpinion(1L, "影像数据清晰，可以进行设计");
    }

    @Test
    void updateEvaluationOpinion_rejectsBlankOpinion() throws Exception {
        mockMvc.perform(post("/design/workorder/{id}/evaluation-opinion", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataEvaluationOpinion\":\" \"}"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(designWorkorderService);
    }

    @Test
    void listWorkorders_delegatesQuery() throws Exception {
        mockMvc.perform(post("/design/workorder/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(designWorkorderService).listWorkorders(any());
    }

    @Test
    void getWorkorderDetail_delegatesOrderId() throws Exception {
        mockMvc.perform(get("/design/workorder/{id}", 1L))
                .andExpect(status().isOk());
        verify(designWorkorderService).getWorkorderDetail(1L);
    }

    @Test
    void assignmentHistory_delegatesOrderId() throws Exception {
        mockMvc.perform(get("/design/workorder/{id}/assignment-history", 1L))
                .andExpect(status().isOk());
        verify(designWorkorderService).listAssignmentHistory(1L);
    }

    @Test
    void workloadExport_delegatesPayloadAndResponse() throws Exception {
        mockMvc.perform(post("/design/workorder/workload/export")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderExportService).exportDesignerWorkload(any(), any());
    }
}
