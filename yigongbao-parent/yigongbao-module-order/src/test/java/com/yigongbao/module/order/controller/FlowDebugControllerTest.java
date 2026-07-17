package com.yigongbao.module.order.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WebMvcTest(FlowDebugController.class)
@ActiveProfiles("test")
class FlowDebugControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private FlowFacade flowFacade;
    @MockBean private FlowStatusHistoryService historyService;
    @MockBean private OrderMainService orderMainService;
    @MockBean private UserService userService;

    @Test
    void preview_rejectsUnknownActionBeforeFlowFacade() throws Exception {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            assertThrows(jakarta.servlet.ServletException.class, () ->
                    mockMvc.perform(get("/api/order/debug/preview")
                            .param("id", "7").param("actionCode", "UNKNOWN")).andReturn());
        }
        verifyNoInteractions(flowFacade);
    }

    @Test
    void history_delegatesOrderId() throws Exception {
        mockMvc.perform(get("/api/order/debug/history").param("id", "7"))
                .andExpect(status().isOk());
        verify(historyService).listByOrderId(7L);
    }

    @Test
    void preview_withValidAction_delegatesFlowFacade() throws Exception {
        when(flowFacade.executeFlow(eq(7L), any(), any()))
                .thenReturn(TransitionResult.of(10, 1010));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            mockMvc.perform(get("/api/order/debug/preview")
                            .param("id", "7").param("actionCode", "SUBMIT_ORDER"))
                    .andExpect(status().isOk());
        }
        verify(flowFacade).executeFlow(eq(7L), any(), any());
    }

    @Test
    void execute_updatesOrderAfterFlowTransition() throws Exception {
        OrderMainEntity entity = new OrderMainEntity();
        entity.setId(8L);
        when(flowFacade.executeFlow(eq(8L), any(), any()))
                .thenReturn(TransitionResult.of(20, 2010));
        when(orderMainService.getById(8L)).thenReturn(entity);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            mockMvc.perform(post("/api/order/debug/execute")
                            .param("id", "8").param("actionCode", "SUBMIT_ORDER")
                            .param("remark", "test"))
                    .andExpect(status().isOk());
        }
        verify(orderMainService).updateById(entity);
    }

    @Test
    void reset_updatesPhaseAndStatus() throws Exception {
        OrderMainEntity entity = new OrderMainEntity();
        entity.setId(9L);
        when(orderMainService.getById(9L)).thenReturn(entity);

        mockMvc.perform(post("/api/order/debug/reset")
                        .param("id", "9").param("phase", "20").param("status", "2010"))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(20, entity.getPhase());
        org.junit.jupiter.api.Assertions.assertEquals(2010, entity.getStatus());
        verify(orderMainService).updateById(entity);
    }
}
