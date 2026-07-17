package com.yigongbao.module.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.service.OrderModifyFullService;
import com.yigongbao.module.order.dto.apply.AuditApplyDTO;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderModifyApplyController.class)
class OrderModifyApplyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderModifyApplyService orderModifyApplyService;
    @MockBean private OrderModifyFullService orderModifyFullService;

    @Test
    void auditApply_rejectsMissingResult() throws Exception {
        mockMvc.perform(put("/order/modify/apply/{id}/audit", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderModifyApplyService);
    }

    @Test
    void auditApply_delegatesResult() throws Exception {
        AuditApplyDTO dto = new AuditApplyDTO();
        dto.setResult(2);
        dto.setRemark("资料不完整");

        mockMvc.perform(put("/order/modify/apply/{id}/audit", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(orderModifyApplyService).auditApply(eq(9L), any(AuditApplyDTO.class));
    }

    @Test
    void fullV2_returnsDecisionFromService() throws Exception {
        when(orderModifyApplyService.modifyOrderFullV2(eq(7L), any(OrderModifyFullDTO.class))).thenReturn(-1);

        mockMvc.perform(put("/order/modify/{id}/full-v2", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderModifyFullDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(-1));

        verify(orderModifyApplyService).modifyOrderFullV2(eq(7L), any(OrderModifyFullDTO.class));
    }

    @Test
    void submitApply_buildsResponseWithExpiry() throws Exception {
        OrderModificationApplyEntity entity = new OrderModificationApplyEntity();
        entity.setExpireTime(LocalDateTime.of(2026, 7, 18, 12, 0));
        when(orderModifyApplyService.submitApply(eq(7L), any(OrderModifyFullDTO.class))).thenReturn(88L);
        when(orderModifyApplyService.getApplyEntityById(88L)).thenReturn(entity);

        mockMvc.perform(post("/order/modify/{id}/apply", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderModifyFullDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applyId").value(88))
                .andExpect(jsonPath("$.data.expireTime").exists());

        verify(orderModifyApplyService).submitApply(eq(7L), any(OrderModifyFullDTO.class));
    }

    @Test
    void full_delegatesToFullModificationService() throws Exception {
        mockMvc.perform(put("/order/modify/{id}/full", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderModifyFullDTO())))
                .andExpect(status().isOk());
        verify(orderModifyFullService).modifyOrderFull(eq(10L), any(OrderModifyFullDTO.class));
    }

    @Test
    void modificationLogs_delegatesOrderAndQuery() throws Exception {
        mockMvc.perform(post("/order/modify/{id}/logs", 11L)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderModifyApplyService).listModificationLogs(eq(11L), any());
    }

    @Test
    void applyQueries_delegateToCorrespondingServices() throws Exception {
        mockMvc.perform(post("/order/modify/apply/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderModifyApplyService).listApplies(any());

        mockMvc.perform(get("/order/modify/apply/{id}", 12L))
                .andExpect(status().isOk());
        verify(orderModifyApplyService).getApplyDetail(12L);

        mockMvc.perform(post("/order/modify/apply/my-list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderModifyApplyService).myListApplies(any());
    }
}
