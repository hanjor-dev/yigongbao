package com.yigongbao.flow.service.impl;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.flow.rules.FlowStatusTransitionRules;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FlowStateMachineServiceImpl 单元测试
 *
 * 【重要】使用真实 FlowStatusTransitionRules 实例：
 * - statusTransitionRules 为真实实例（非 @Mock）
 * - 通过构建特定的 testOrder 状态使真实规则返回预期动作
 * - 不依赖任何 stub，由真实规则类提供业务逻辑
 *
 * @author hanjor
 * @date 2026-04-02
 */
@DisplayName("FlowStateMachineServiceImpl 单元测试")
class FlowStateMachineServiceImplTest {

    private FlowOrderService flowOrderService;
    private FlowStatusTransitionRules statusTransitionRules;
    private FlowStatusHistoryService flowStatusHistoryService;
    private FlowStateMachineServiceImpl flowStateMachineService;

    private OrderMainEntity testOrder;
    private FlowOperator testOperator;

    @BeforeEach
    void setUp() {
        flowOrderService = mock(FlowOrderService.class);
        flowStatusHistoryService = mock(FlowStatusHistoryService.class);
        statusTransitionRules = new FlowStatusTransitionRules();

        flowStateMachineService = new FlowStateMachineServiceImpl(
                flowOrderService, statusTransitionRules, flowStatusHistoryService);

        testOperator = new FlowOperator(1L, "测试用户", null);
        testOrder = new OrderMainEntity();
        testOrder.setId(1L);
        testOrder.setOrderCode("ORD-20260402-0001");
    }

    // ==================== CREATE 动作测试 ====================

    @Nested
    @DisplayName("CREATE 动作")
    class CreateActionTests {

        @Test
        @DisplayName("CREATE 动作 → 仅记录历史，phase/status 不变")
        void createAction_shouldOnlyRecordHistory() {
            testOrder.setPhase(1);
            testOrder.setStatus(10);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.CREATE, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(1, result.getTargetPhase());
            assertEquals(10, result.getTargetStatus());
            assertNull(result.getInitialStatus());

            verify(flowStatusHistoryService).recordTransition(
                    eq(1L), eq("ORD-20260402-0001"), eq(1),
                    eq(10), eq(10),
                    eq("CREATE"), eq("创建订单"),
                    eq(testOperator));
        }
    }

    // ==================== 提交/撤回测试 ====================

    @Nested
    @DisplayName("提交/撤回动作")
    class SubmitAndWithdrawTests {

        @Test
        @DisplayName("SUBMIT_ORDER: DRAFT(10) → PENDING_DATA_AUDIT(11)")
        void submitOrder_shouldChangeToPendingAudit() {
            // DRAFT(10) 在 ORDER 阶段允许 SUBMIT_ORDER
            testOrder.setPhase(1);
            testOrder.setStatus(10);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.SUBMIT_ORDER, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(1, result.getTargetPhase());
            assertEquals(11, result.getTargetStatus());
        }

        @Test
        @DisplayName("WITHDRAW: DATA_AUDIT_PASSED(12) → PENDING_DATA_AUDIT(11)")
        void withdraw_shouldReturnToPendingAudit() {
            // WITHDRAW 在 ORDER 阶段仅允许从 DATA_AUDIT_PASSED(12) 执行
            testOrder.setPhase(1);
            testOrder.setStatus(12);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.WITHDRAW, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(1, result.getTargetPhase());
            assertEquals(11, result.getTargetStatus());
        }

        @Test
        @DisplayName("RESUBMIT: DATA_AUDIT_REJECTED(13) → PENDING_DATA_AUDIT(11)")
        void resubmit_shouldChangeToPendingAudit() {
            // RESUBMIT 在 ORDER 阶段仅允许从 DATA_AUDIT_REJECTED(13) 执行
            testOrder.setPhase(1);
            testOrder.setStatus(13);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.RESUBMIT, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(1, result.getTargetPhase());
            assertEquals(11, result.getTargetStatus());
        }
    }

    // ==================== 审核通过推进阶段测试（核心）====================

    @Nested
    @DisplayName("DATA_AUDIT_PASS 推进阶段（核心）")
    class AuditPassPhaseChangeTests {

        @Test
        @DisplayName("DATA_AUDIT_PASS: phase=1,status=11 → phase=2,status=21")
        void auditPass_shouldAdvancePhaseToDesign() {
            // PENDING_DATA_AUDIT(11) 在 ORDER 阶段允许 DATA_AUDIT_PASS
            testOrder.setPhase(1);
            testOrder.setStatus(11);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DATA_AUDIT_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(2, result.getTargetPhase());
            assertEquals(12, result.getTargetStatus()); // DATA_AUDIT_PASSED
            assertEquals(21, result.getInitialStatus()); // DESIGNING
            assertEquals(21, result.getFinalStatus());
        }
    }

    // ==================== 审核驳回测试 ====================

    @Nested
    @DisplayName("DATA_AUDIT_REJECT 审核驳回")
    class AuditRejectTests {

        @Test
        @DisplayName("DATA_AUDIT_REJECT: phase=1,status=11 → phase=1,status=13（不推进阶段）")
        void auditReject_shouldReturnToDraft() {
            // PENDING_DATA_AUDIT(11) 在 ORDER 阶段允许 DATA_AUDIT_REJECT
            testOrder.setPhase(1);
            testOrder.setStatus(11);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DATA_AUDIT_REJECT, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(1, result.getTargetPhase());
            assertEquals(13, result.getTargetStatus()); // DATA_AUDIT_REJECTED
            assertEquals(13, result.getFinalStatus());
        }
    }

    // ==================== 不可见状态自动吸收测试（核心）====================

    @Nested
    @DisplayName("不可见状态自动吸收（核心）")
    class InvisibleStatusTests {

        @Test
        @DisplayName("DESIGN_REVIEW_PASS: phase=2,status=23,needsPhysicalDelivery=0 → phase=7,finalStatus=71")
        void designReviewPass_noDelivery_shouldAbsorbAndAdvanceToConfirm() {
            // DESIGN_REVIEWING(23) 在 DESIGN 阶段允许 DESIGN_REVIEW_PASS
            testOrder.setPhase(2);
            testOrder.setStatus(23);
            testOrder.setNeedsPhysicalDelivery(0);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DESIGN_REVIEW_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(7, result.getTargetPhase()); // CONFIRM 阶段
            assertEquals(24, result.getTargetStatus()); // DESIGN_REVIEW_PASSED（不可见）
            assertEquals(71, result.getInitialStatus()); // AWAITING_CONFIRM
            assertEquals(71, result.getFinalStatus());
        }

        @Test
        @DisplayName("DESIGN_REVIEW_PASS: phase=2,status=23,needsPhysicalDelivery=1 → phase=3,finalStatus=31")
        void designReviewPass_needDelivery_shouldAdvanceToPrint() {
            // DESIGN_REVIEWING(23) 在 DESIGN 阶段允许 DESIGN_REVIEW_PASS
            testOrder.setPhase(2);
            testOrder.setStatus(23);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DESIGN_REVIEW_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(3, result.getTargetPhase()); // PRINT 阶段
            assertEquals(24, result.getTargetStatus()); // DESIGN_REVIEW_PASSED（不可见）
            assertEquals(31, result.getInitialStatus()); // PENDING_PRINT
            assertEquals(31, result.getFinalStatus());
        }
    }

    // ==================== 打印完成推进阶段测试 ====================

    @Nested
    @DisplayName("COMPLETE_PRINT 推进阶段")
    class CompletePrintTests {

        @Test
        @DisplayName("COMPLETE_PRINT: phase=3,status=32 → phase=4,status=41")
        void completePrint_shouldAdvanceToPostProcessing() {
            // PRINTING(32) 在 PRINT 阶段允许 COMPLETE_PRINT
            testOrder.setPhase(3);
            testOrder.setStatus(32);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.COMPLETE_PRINT, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(4, result.getTargetPhase());
            assertEquals(33, result.getTargetStatus()); // PRINT_COMPLETED（过渡）
            assertEquals(41, result.getInitialStatus()); // POST_PROCESSING
            assertEquals(41, result.getFinalStatus());
        }
    }

    // ==================== 质检合格推进阶段测试 ====================

    @Nested
    @DisplayName("QC_PASS 推进阶段")
    class QcPassTests {

        @Test
        @DisplayName("QC_PASS: phase=5,status=51 → phase=6,status=61")
        void qcPass_shouldAdvanceToWarehouse() {
            // QC_IN_PROGRESS(51) 在 QC 阶段允许 QC_PASS
            testOrder.setPhase(5);
            testOrder.setStatus(51);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.QC_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(6, result.getTargetPhase());
            assertEquals(52, result.getTargetStatus()); // QC_PASSED（过渡）
            assertEquals(61, result.getInitialStatus()); // WAREHOUSE_IN
            assertEquals(61, result.getFinalStatus());
        }
    }

    // ==================== 入库完成测试 ====================

    @Nested
    @DisplayName("COMPLETE_WAREHOUSE_IN 入库完成")
    class CompleteWarehouseInTests {

        @Test
        @DisplayName("COMPLETE_WAREHOUSE_IN: phase=6,status=61 → phase=8,status=80")
        void completeWarehouseIn_shouldAdvanceToCompleted() {
            // WAREHOUSE_IN(61) 在 WAREHOUSE 阶段允许 COMPLETE_WAREHOUSE_IN
            testOrder.setPhase(6);
            testOrder.setStatus(61);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.COMPLETE_WAREHOUSE_IN, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(8, result.getTargetPhase());
            assertEquals(62, result.getTargetStatus()); // WAREHOUSED（过渡）
            assertEquals(80, result.getInitialStatus()); // COMPLETED
            assertEquals(80, result.getFinalStatus());
        }
    }

    // ==================== 客户确认测试 ====================

    @Nested
    @DisplayName("USER_CONFIRM 客户确认")
    class UserConfirmTests {

        @Test
        @DisplayName("USER_CONFIRM: phase=7,status=71 → phase=8,status=80")
        void userConfirm_shouldAdvanceToCompleted() {
            // AWAITING_CONFIRM(71) 在 CONFIRM 阶段允许 USER_CONFIRM
            testOrder.setPhase(7);
            testOrder.setStatus(71);
            testOrder.setNeedsPhysicalDelivery(0);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.USER_CONFIRM, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(8, result.getTargetPhase());
            assertEquals(80, result.getTargetStatus()); // COMPLETED
            assertEquals(80, result.getInitialStatus());
            assertEquals(80, result.getFinalStatus());
        }
    }

    // ==================== 循环次数超限测试 ====================

    @Nested
    @DisplayName("循环次数超限")
    class ExcessiveLoopTests {

        @Test
        @DisplayName("驳回次数已达上限（10次）→ 抛出 ORDER_EXCESSIVE_AUDIT_REJECT")
        void auditRejectExceedLimit_shouldThrow() {
            // 模拟历史中已有10次 DATA_AUDIT_REJECT
            testOrder.setPhase(1);
            testOrder.setStatus(11);
            testOrder.setNeedsPhysicalDelivery(1);
            java.util.List<String> historyWith10Rejects = Collections.nCopies(10, "DATA_AUDIT_REJECT");
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(historyWith10Rejects);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> flowStateMachineService.executeTransition(
                            1L, FlowActionEnum.DATA_AUDIT_REJECT, testOperator)
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("驳回次数达边界值（9次）→ 不抛异常，正常执行")
        void auditRejectAtBoundary_shouldNotThrow() {
            // 模拟历史中已有9次驳回（在边界内）
            testOrder.setPhase(1);
            testOrder.setStatus(11);
            testOrder.setNeedsPhysicalDelivery(1);
            java.util.List<String> historyWith9Rejects = Collections.nCopies(9, "DATA_AUDIT_REJECT");
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(historyWith9Rejects);

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DATA_AUDIT_REJECT, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(1, result.getTargetPhase());
            assertEquals(13, result.getFinalStatus());
        }
    }

    // ==================== 非法动作测试 ====================

    @Nested
    @DisplayName("非法动作")
    class InvalidActionTests {

        @Test
        @DisplayName("在不允许的状态下执行动作 → 抛出 ORDER_STATUS_TRANSITION_ERROR")
        void invalidAction_shouldThrow() {
            // DRAFT(10) 不允许 DATA_AUDIT_PASS（允许 SUBMIT_ORDER）
            testOrder.setPhase(1);
            testOrder.setStatus(10);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> flowStateMachineService.executeTransition(
                            1L, FlowActionEnum.DATA_AUDIT_PASS, testOperator)
            );
            assertEquals(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR.getCode(), ex.getCode());
        }
    }

    // ==================== 订单不存在测试 ====================

    @Nested
    @DisplayName("订单不存在")
    class OrderNotFoundTests {

        @Test
        @DisplayName("flowOrderService.getById 返回 null → 抛出 ORDER_NOT_FOUND")
        void orderNotFound_shouldThrow() {
            when(flowOrderService.getById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> flowStateMachineService.executeTransition(
                            999L, FlowActionEnum.SUBMIT_ORDER, testOperator)
            );
            assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ==================== phase 为 null 防御测试 ====================

    @Nested
    @DisplayName("phase 为 null 防御")
    class NullPhaseTests {

        @Test
        @DisplayName("订单 phase=null → 抛出 ORDER_STATUS_TRANSITION_ERROR")
        void nullPhase_shouldThrow() {
            testOrder.setPhase(null);
            testOrder.setStatus(10);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> flowStateMachineService.executeTransition(
                            1L, FlowActionEnum.SUBMIT_ORDER, testOperator)
            );
            assertEquals(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR.getCode(), ex.getCode());
        }
    }
}
