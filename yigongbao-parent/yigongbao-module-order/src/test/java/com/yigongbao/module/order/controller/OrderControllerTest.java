package com.yigongbao.module.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.OrderDraftService;
import com.yigongbao.module.order.service.OrderExportService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CancelOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.ManualCompleteOrderDTO;
import com.yigongbao.module.order.dto.order.ResubmitOrderDTO;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrderDraftService orderDraftService;
    @MockBean private OrderMainService orderMainService;
    @MockBean private OrderExportService orderExportService;
    @MockBean private DesignerAssignmentService designerAssignmentService;

    @Test
    void createOrder_rejectsInvalidRequestBeforeService() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderDTO())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderMainService);
    }

    @Test
    void submitOrder_delegatesPathId() throws Exception {
        mockMvc.perform(post("/order/{id}/submit", 17L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderMainService).submitOrder(17L);
    }

    @Test
    void withdrawOrder_delegatesPathId() throws Exception {
        mockMvc.perform(post("/order/{id}/withdraw", 18L))
                .andExpect(status().isOk());

        verify(orderMainService).withdrawOrder(18L);
    }

    @Test
    void auditReject_requiresValidatedPayload() throws Exception {
        AuditOrderDTO dto = new AuditOrderDTO();
        dto.setVersion(1);
        dto.setRemark("资料不完整");

        mockMvc.perform(post("/order/{id}/audit-reject", 19L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(orderMainService).auditReject(eq(19L), any(AuditOrderDTO.class));
    }

    @Test
    void resubmit_passesVersionFromPayload() throws Exception {
        ResubmitOrderDTO dto = new ResubmitOrderDTO();
        dto.setVersion(3);

        mockMvc.perform(post("/order/{id}/resubmit", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(orderMainService).resubmit(20L, 3);
    }

    @Test
    void cancel_passesVersionFromPayload() throws Exception {
        CancelOrderDTO dto = new CancelOrderDTO();
        dto.setVersion(4);

        mockMvc.perform(post("/order/{id}/cancel", 21L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(orderMainService).cancelOrder(21L, 4);
    }

    @Test
    void manualComplete_passesVersionFromPayload() throws Exception {
        ManualCompleteOrderDTO dto = new ManualCompleteOrderDTO();
        dto.setVersion(5);

        mockMvc.perform(post("/order/{id}/manual-complete", 22L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(orderMainService).manualCompleteOrder(22L, 5);
    }

    @Test
    void availableExportFields_delegatesService() throws Exception {
        when(orderExportService.getAvailableExportFields()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/order/export/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderExportService).getAvailableExportFields();
    }

    @Test
    void draftList_delegatesQuery() throws Exception {
        mockMvc.perform(post("/order/draft/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderDraftService).listDrafts(any());
    }

    @Test
    void draftDetail_validatesOwnerThenDelegatesDetail() throws Exception {
        try (org.mockito.MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            mockMvc.perform(get("/order/draft/{id}", 30L))
                    .andExpect(status().isOk());
        }
        verify(orderDraftService).validateDraftOwner(30L, 7L);
        verify(orderDraftService).getDraftDetail(30L);
    }

    @Test
    void saveDraft_delegatesPayload() throws Exception {
        mockMvc.perform(post("/order/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderType\":1,\"needsPhysicalDelivery\":0,\"businessType\":\"11.1\",\"orgId\":1,\"hospitalId\":2,\"patientName\":\"测试\"}"))
                .andExpect(status().isOk());
        verify(orderDraftService).saveDraft(any());
    }

    @Test
    void saveDraft_acceptsRemotePrintingValue() throws Exception {
        mockMvc.perform(post("/order/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderType\":1,\"needsPhysicalDelivery\":2,\"businessType\":\"11.1\",\"orgId\":1,\"hospitalId\":2,\"patientName\":\"测试\"}"))
                .andExpect(status().isOk());
        verify(orderDraftService).saveDraft(any());
    }

    @Test
    void removeDraft_delegatesPathId() throws Exception {
        mockMvc.perform(delete("/order/draft/{id}", 31L))
                .andExpect(status().isOk());
        verify(orderDraftService).removeDraft(31L);
    }

    @Test
    void createOrder_delegatesValidPayload() throws Exception {
        when(orderMainService.createOrder(any())).thenReturn(32L);
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderType\":1,\"needsPhysicalDelivery\":0,\"businessType\":\"11.1\",\"orgId\":1,\"hospitalId\":2,\"patientName\":\"测试\",\"items\":[{\"bodyPartId\":1,\"projectId\":2}]}"))
                .andExpect(status().isOk());
        verify(orderMainService).createOrder(any());
    }

    @Test
    void createOrder_acceptsRemotePrintingValue() throws Exception {
        when(orderMainService.createOrder(any())).thenReturn(33L);
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderType\":1,\"needsPhysicalDelivery\":2,\"businessType\":\"11.1\",\"orgId\":1,\"hospitalId\":2,\"patientName\":\"测试\",\"items\":[{\"bodyPartId\":1,\"projectId\":2}]}"))
                .andExpect(status().isOk());
        verify(orderMainService).createOrder(any());
    }

    @Test
    void createOrder_rejectsUnsupportedPhysicalDeliveryValue() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderType\":1,\"needsPhysicalDelivery\":3,\"businessType\":\"11.1\",\"orgId\":1,\"hospitalId\":2,\"patientName\":\"测试\",\"items\":[{\"bodyPartId\":1,\"projectId\":2}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderMainService);
    }

    @Test
    void listOrders_delegatesQuery() throws Exception {
        mockMvc.perform(post("/order/page")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderMainService).listOrders(any());
    }

    @Test
    void statistics_delegatesService() throws Exception {
        mockMvc.perform(get("/order/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderMainService).statistics();
    }

    @Test
    void orderDetail_delegatesPathId() throws Exception {
        mockMvc.perform(get("/order/{id}", 33L)).andExpect(status().isOk());
        verify(orderMainService).getOrderDetail(33L);
    }

    @Test
    void auditPass_delegatesPayload() throws Exception {
        AuditOrderDTO dto = new AuditOrderDTO();
        dto.setVersion(1);
        mockMvc.perform(post("/order/{id}/audit-pass", 34L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(orderMainService).auditPass(eq(34L), any(AuditOrderDTO.class));
    }

    @Test
    void availableActions_delegatesPathId() throws Exception {
        when(orderMainService.listAvailableActions(35L)).thenReturn(java.util.List.of());
        mockMvc.perform(get("/order/{id}/actions", 35L)).andExpect(status().isOk());
        verify(orderMainService).listAvailableActions(35L);
    }

    @Test
    void availableDesigners_delegatesQuery() throws Exception {
        mockMvc.perform(post("/order/designers/available")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(designerAssignmentService).listAvailableDesigners(any());
    }

    @Test
    void assignDesigner_delegatesPathAndDesignerId() throws Exception {
        mockMvc.perform(post("/order/{id}/assign-designer", 36L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designerId\":99}"))
                .andExpect(status().isOk());
        verify(designerAssignmentService).manualAssignDesigner(36L, 99L);
    }

    @Test
    void columnConfig_getSaveAndReset_delegateService() throws Exception {
        mockMvc.perform(get("/order/column-config")).andExpect(status().isOk());
        verify(orderMainService).getColumnConfig();

        mockMvc.perform(put("/order/column-config")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderMainService).saveColumnConfig(any());

        mockMvc.perform(delete("/order/column-config")).andExpect(status().isOk());
        verify(orderMainService).resetColumnConfig();
    }

    @Test
    void exportEndpoints_delegateService() throws Exception {
        mockMvc.perform(post("/order/export")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(orderExportService).exportOrders(any(), any());

        mockMvc.perform(post("/order/export/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportFields\":[\"orderCode\"]}"))
                .andExpect(status().isOk());
        verify(orderExportService).customExportOrders(any(), any());
    }
}
