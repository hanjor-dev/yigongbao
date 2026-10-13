package com.yigongbao.flow.facade.impl;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.flow.service.FlowStateMachineService;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowFacadeImplTest {

    @Test
    void startsAdditionalDesignWithHistoryAndVersionGuard() {
        FlowStateMachineService stateMachine = mock(FlowStateMachineService.class);
        FlowOrderService orderService = mock(FlowOrderService.class);
        FlowStatusHistoryService historyService = mock(FlowStatusHistoryService.class);
        FlowFacade facade = new FlowFacadeImpl(stateMachine, orderService, historyService);

        OrderMainEntity order = new OrderMainEntity();
        order.setId(10L);
        order.setOrderCode("ORD-10");
        order.setStatus(6010);
        order.setPhase(60);
        order.setVersion(7);
        when(orderService.getById(10L)).thenReturn(order);
        when(orderService.updatePhaseAndStatusWithHandlerIfVersion(
                10L, 7, 20, 2020, 3L, "设计师")).thenReturn(1);

        var result = facade.executeAdditionalDesignStart(
                10L, new FlowOperator(3L, "设计师", "批次=AD-10-1"), 7);

        assertEquals(20, result.getTargetPhase());
        assertEquals(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(), result.getTargetStatus());
        verify(historyService).recordTransition(
                10L, "ORD-10", FlowPhaseEnum.DESIGN.getValue(), 6010, 2020,
                FlowActionEnum.START_ADDITIONAL_DESIGN.getCode(),
                FlowActionEnum.START_ADDITIONAL_DESIGN.getName(),
                new FlowOperator(3L, "设计师", "批次=AD-10-1"));
        verify(orderService).updatePhaseAndStatusWithHandlerIfVersion(
                10L, 7, 20, 2020, 3L, "设计师");
    }

    @Test
    void rejectsAdditionalDesignBatchWhenVersionWasChanged() {
        FlowStateMachineService stateMachine = mock(FlowStateMachineService.class);
        FlowOrderService orderService = mock(FlowOrderService.class);
        FlowStatusHistoryService historyService = mock(FlowStatusHistoryService.class);
        FlowFacade facade = new FlowFacadeImpl(stateMachine, orderService, historyService);

        OrderMainEntity order = new OrderMainEntity();
        order.setId(10L);
        order.setOrderCode("ORD-10");
        order.setStatus(8010);
        order.setPhase(80);
        order.setVersion(2);
        when(orderService.getById(10L)).thenReturn(order);
        when(orderService.updatePhaseAndStatusWithHandlerIfVersion(
                10L, 2, 20, 2020, 3L, "设计师")).thenReturn(0);

        assertThrows(RuntimeException.class, () -> facade.executeAdditionalDesignStart(
                10L, new FlowOperator(3L, "设计师", null), 2));
        verify(orderService).updatePhaseAndStatusWithHandlerIfVersion(
                10L, 2, 20, 2020, 3L, "设计师");
    }
}
