package com.yigongbao.module.order.service.impl;

import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderModifyDirectServiceTest {

    @InjectMocks
    private OrderModifyApplyServiceImpl service;

    @Mock
    private FlowFacade flowFacade;

    // ==================== determineAllowedTypesByPhase ====================

    @Test
    void testDetermineAllowedTypesByPhase_OrderPhase_ReturnsAllTypes() {
        Set<String> result = service.determineAllowedTypesByPhase(FlowPhaseEnum.ORDER.getValue());

        assertEquals(3, result.size());
        assertTrue(result.contains("14.1"));
        assertTrue(result.contains("14.2"));
        assertTrue(result.contains("14.3"));
    }

    @Test
    void testDetermineAllowedTypesByPhase_DesignPhase_ReturnsItemOnly() {
        Set<String> result = service.determineAllowedTypesByPhase(FlowPhaseEnum.DESIGN.getValue());

        assertEquals(1, result.size());
        assertTrue(result.contains("14.3"));
    }

    @Test
    void testDetermineAllowedTypesByPhase_InvalidPhase_ThrowsException() {
        assertThrows(BusinessException.class, () -> service.determineAllowedTypesByPhase(30));
    }

    // ==================== buildModificationsMap ====================

    @Test
    void testBuildModificationsMap_WithInfoFields_ReturnsCorrectMap() {
        ExecuteModifyDTO dto = new ExecuteModifyDTO();
        ExecuteModifyDTO.ModifyField field = new ExecuteModifyDTO.ModifyField();
        field.setField("patientName");
        field.setValue("张三");
        dto.setInfoFields(List.of(field));

        Map<String, Object> result = service.buildModificationsMap(dto);

        assertNotNull(result);
        assertTrue(result.containsKey("patientName"));
        assertEquals("张三", result.get("patientName"));
    }

    // ==================== triggerPostModifyFlow ====================

    private void invokeTriggerPostModifyFlow(Long orderId, Integer phase, Integer status) throws Exception {
        Method m = OrderModifyApplyServiceImpl.class.getDeclaredMethod(
                "triggerPostModifyFlow", Long.class, Integer.class, Integer.class, Long.class, String.class);
        m.setAccessible(true);
        m.invoke(service, orderId, phase, status, 1L, "测试员");
    }

    @Test
    void triggerPostModifyFlow_OrderPhase_DataAuditRejected_CallsResubmit() throws Exception {
        invokeTriggerPostModifyFlow(1L,
                FlowPhaseEnum.ORDER.getValue(),
                FlowStatusEnum.DATA_AUDIT_REJECTED.getValue());

        verify(flowFacade, times(1)).executeFlow(
                eq(1L), eq(FlowActionEnum.RESUBMIT), any(FlowOperator.class));
        verifyNoMoreInteractions(flowFacade);
    }

    @Test
    void triggerPostModifyFlow_OrderPhase_PendingDataAudit_NoFlow() throws Exception {
        invokeTriggerPostModifyFlow(1L,
                FlowPhaseEnum.ORDER.getValue(),
                FlowStatusEnum.PENDING_DATA_AUDIT.getValue());

        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }

    @Test
    void triggerPostModifyFlow_DesignPhase_DesignReviewRejected_CallsTwoActions() throws Exception {
        invokeTriggerPostModifyFlow(1L,
                FlowPhaseEnum.DESIGN.getValue(),
                FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue());

        verify(flowFacade, times(1)).executeFlow(
                eq(1L), eq(FlowActionEnum.CONTINUE_DESIGN), any(FlowOperator.class));
        verify(flowFacade, times(1)).executeFlow(
                eq(1L), eq(FlowActionEnum.SUBMIT_DESIGN), any(FlowOperator.class));
        verifyNoMoreInteractions(flowFacade);
    }

    @Test
    void triggerPostModifyFlow_DesignPhase_DesignInProgress_NoFlow() throws Exception {
        invokeTriggerPostModifyFlow(1L,
                FlowPhaseEnum.DESIGN.getValue(),
                FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());

        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }

    @Test
    void triggerPostModifyFlow_DesignPhase_DesignReviewing_NoFlow() throws Exception {
        invokeTriggerPostModifyFlow(1L,
                FlowPhaseEnum.DESIGN.getValue(),
                FlowStatusEnum.DESIGN_REVIEWING.getValue());

        verify(flowFacade, never()).executeFlow(any(), any(), any());
    }
}
