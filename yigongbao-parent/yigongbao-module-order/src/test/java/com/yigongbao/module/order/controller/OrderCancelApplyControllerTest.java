package com.yigongbao.module.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.order.dto.order.AuditCancelApplyDTO;
import com.yigongbao.module.order.dto.order.CancelOrderApplyDTO;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.vo.order.CancelApplyVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderCancelApplyController Web层测试
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@WebMvcTest(OrderCancelApplyController.class)
class OrderCancelApplyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderCancelApplyService cancelApplyService;

    private static final Long APPLY_ID = 3001L;
    private static final Long ORDER_ID = 2001L;

    /**
     * 测试提交取消申请 - 成功
     */
    @Test
    void submitCancelApply_Success() throws Exception {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        dto.setOrderId(ORDER_ID);
        dto.setReason("客户取消订单");

        when(cancelApplyService.submitCancelApply(any(CancelOrderApplyDTO.class)))
                .thenReturn(APPLY_ID);

        // Act & Assert
        mockMvc.perform(post("/order/cancel-apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(APPLY_ID));

        verify(cancelApplyService, times(1)).submitCancelApply(any(CancelOrderApplyDTO.class));
    }

    /**
     * 测试提交取消申请 - 参数校验失败（订单ID为空）
     */
    @Test
    void submitCancelApply_ValidationFailed() throws Exception {
        // Arrange
        CancelOrderApplyDTO dto = new CancelOrderApplyDTO();
        // orderId 为空，应该触发校验失败

        // Act & Assert
        mockMvc.perform(post("/order/cancel-apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(cancelApplyService, never()).submitCancelApply(any());
    }

    /**
     * 测试审核取消申请 - 审核通过
     */
    @Test
    void auditCancelApply_Approved() throws Exception {
        // Arrange
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        dto.setApproved(true);
        dto.setReason("审核通过");

        doNothing().when(cancelApplyService).auditCancelApply(eq(APPLY_ID), any(AuditCancelApplyDTO.class));

        // Act & Assert
        mockMvc.perform(post("/order/cancel-apply/{applyId}/audit", APPLY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(cancelApplyService, times(1)).auditCancelApply(eq(APPLY_ID), any(AuditCancelApplyDTO.class));
    }

    /**
     * 测试审核取消申请 - 审核驳回
     */
    @Test
    void auditCancelApply_Rejected() throws Exception {
        // Arrange
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        dto.setApproved(false);
        dto.setReason("不符合取消条件");

        doNothing().when(cancelApplyService).auditCancelApply(eq(APPLY_ID), any(AuditCancelApplyDTO.class));

        // Act & Assert
        mockMvc.perform(post("/order/cancel-apply/{applyId}/audit", APPLY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(cancelApplyService, times(1)).auditCancelApply(eq(APPLY_ID), any(AuditCancelApplyDTO.class));
    }

    /**
     * 测试审核取消申请 - 参数校验失败（审核结果为空）
     */
    @Test
    void auditCancelApply_ValidationFailed() throws Exception {
        // Arrange
        AuditCancelApplyDTO dto = new AuditCancelApplyDTO();
        // approved 为空，应该触发校验失败

        // Act & Assert
        mockMvc.perform(post("/order/cancel-apply/{applyId}/audit", APPLY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(cancelApplyService, never()).auditCancelApply(any(), any());
    }

    /**
     * 测试查询取消申请详情 - 成功
     */
    @Test
    void getCancelApplyDetail_Success() throws Exception {
        // Arrange
        CancelApplyVO vo = new CancelApplyVO();
        vo.setId(APPLY_ID);
        vo.setOrderId(ORDER_ID);
        vo.setOrderCode("ORD2026071000001");
        vo.setApplyReason("客户取消订单");

        when(cancelApplyService.getCancelApplyDetail(APPLY_ID)).thenReturn(vo);

        // Act & Assert
        mockMvc.perform(get("/order/cancel-apply/{applyId}", APPLY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(APPLY_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID));

        verify(cancelApplyService, times(1)).getCancelApplyDetail(APPLY_ID);
    }

    @Test
    void listPendingApplies_delegatesQuery() throws Exception {
        mockMvc.perform(post("/order/cancel-apply/pending/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(cancelApplyService).listPendingApplies(any());
    }

    @Test
    void listMyApplies_delegatesQuery() throws Exception {
        mockMvc.perform(post("/order/cancel-apply/my-applies")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(cancelApplyService).listMyApplies(any());
    }

    @Test
    void history_delegatesOrderId() throws Exception {
        mockMvc.perform(get("/order/cancel-apply/order/{orderId}/history", ORDER_ID))
                .andExpect(status().isOk());
        verify(cancelApplyService).getCancelApplyHistory(ORDER_ID);
    }
}
