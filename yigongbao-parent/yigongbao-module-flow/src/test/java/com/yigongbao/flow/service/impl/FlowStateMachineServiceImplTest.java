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
            testOrder.setPhase(10);
            testOrder.setStatus(1001);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.CREATE, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(10, result.getTargetPhase());
            assertEquals(1001, result.getTargetStatus());
            assertNull(result.getInitialStatus());

            verify(flowStatusHistoryService).recordTransition(
                    eq(1L), eq("ORD-20260402-0001"), eq(10),
                    eq(1001), eq(1001),
                    eq("CREATE"), eq("创建订单"),
                    eq(testOperator));
        }
    }

    // ==================== 提交/撤回测试 ====================

    @Nested
    @DisplayName("提交/撤回动作")
    class SubmitAndWithdrawTests {

        @Test
        @DisplayName("SUBMIT_ORDER: DRAFT(1001) → PENDING_DATA_AUDIT(1002)")
        void submitOrder_shouldChangeToPendingAudit() {
            // DRAFT(1001) 在 ORDER 阶段允许 SUBMIT_ORDER
            testOrder.setPhase(10);
            testOrder.setStatus(1001);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.SUBMIT_ORDER, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(10, result.getTargetPhase());
            assertEquals(1002, result.getTargetStatus());
        }

        @Test
        @DisplayName("WITHDRAW: DATA_AUDIT_PASSED(1003) → PENDING_DATA_AUDIT(1002)")
        void withdraw_shouldReturnToPendingAudit() {
            // WITHDRAW 在 ORDER 阶段仅允许从 DATA_AUDIT_PASSED(1003) 执行
            testOrder.setPhase(10);
            testOrder.setStatus(1003);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.WITHDRAW, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(10, result.getTargetPhase());
            assertEquals(1002, result.getTargetStatus());
        }

        @Test
        @DisplayName("RESUBMIT: DATA_AUDIT_REJECTED(1004) → PENDING_DATA_AUDIT(1002)")
        void resubmit_shouldChangeToPendingAudit() {
            // RESUBMIT 在 ORDER 阶段仅允许从 DATA_AUDIT_REJECTED(1004) 执行
            testOrder.setPhase(10);
            testOrder.setStatus(1004);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.RESUBMIT, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(10, result.getTargetPhase());
            assertEquals(1002, result.getTargetStatus());
        }
    }

    // ==================== 审核通过推进阶段测试（核心）====================

    @Nested
    @DisplayName("DATA_AUDIT_PASS 推进阶段（核心）")
    class AuditPassPhaseChangeTests {

        @Test
        @DisplayName("DATA_AUDIT_PASS: phase=10,status=1002 → phase=20,status=2001")
        void auditPass_shouldAdvancePhaseToDesign() {
            // PENDING_DATA_AUDIT(1002) 在 ORDER 阶段允许 DATA_AUDIT_PASS
            testOrder.setPhase(10);
            testOrder.setStatus(1002);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DATA_AUDIT_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(20, result.getTargetPhase());
            assertEquals(1003, result.getTargetStatus()); // DATA_AUDIT_PASSED
            assertEquals(2001, result.getInitialStatus()); // PENDING_DESIGN
            assertEquals(2001, result.getFinalStatus());
        }
    }

    // ==================== 审核驳回测试 ====================

    @Nested
    @DisplayName("DATA_AUDIT_REJECT 审核驳回")
    class AuditRejectTests {

        @Test
        @DisplayName("DATA_AUDIT_REJECT: phase=10,status=1002 → phase=10,status=1004（不推进阶段）")
        void auditReject_shouldReturnToDraft() {
            // PENDING_DATA_AUDIT(1002) 在 ORDER 阶段允许 DATA_AUDIT_REJECT
            testOrder.setPhase(10);
            testOrder.setStatus(1002);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DATA_AUDIT_REJECT, testOperator);

            assertFalse(result.isPhaseChanged());
            assertEquals(10, result.getTargetPhase());
            assertEquals(1004, result.getTargetStatus()); // DATA_AUDIT_REJECTED
            assertEquals(1004, result.getFinalStatus());
        }
    }

    // ==================== 不可见状态自动吸收测试（核心）====================

    @Nested
    @DisplayName("不可见状态自动吸收（核心）")
    class InvisibleStatusTests {

        @Test
        @DisplayName("DESIGN_REVIEW_PASS: phase=20,status=2004,needsPhysicalDelivery=0 → phase=70,finalStatus=7001")
        void designReviewPass_noDelivery_shouldAbsorbAndAdvanceToConfirm() {
            // DESIGN_REVIEWING(2004) 在 DESIGN 阶段允许 DESIGN_REVIEW_PASS
            testOrder.setPhase(20);
            testOrder.setStatus(2004);
            testOrder.setNeedsPhysicalDelivery(0);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DESIGN_REVIEW_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(70, result.getTargetPhase()); // CONFIRM 阶段
            assertEquals(2005, result.getTargetStatus()); // DESIGN_REVIEW_PASSED（不可见）
            assertEquals(7001, result.getInitialStatus()); // AWAITING_CONFIRM
            assertEquals(7001, result.getFinalStatus());
        }

        @Test
        @DisplayName("DESIGN_REVIEW_PASS: phase=20,status=2004,needsPhysicalDelivery=1 → phase=30,finalStatus=3001")
        void designReviewPass_needDelivery_shouldAdvanceToPrint() {
            // DESIGN_REVIEWING(2004) 在 DESIGN 阶段允许 DESIGN_REVIEW_PASS
            testOrder.setPhase(20);
            testOrder.setStatus(2004);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.DESIGN_REVIEW_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(30, result.getTargetPhase()); // PRINT 阶段
            assertEquals(2005, result.getTargetStatus()); // DESIGN_REVIEW_PASSED（不可见）
            assertEquals(3001, result.getInitialStatus()); // PENDING_PRINT
            assertEquals(3001, result.getFinalStatus());
        }
    }

    // ==================== 打印完成推进阶段测试 ====================

    @Nested
    @DisplayName("COMPLETE_PRINT 推进阶段")
    class CompletePrintTests {

        @Test
        @DisplayName("COMPLETE_PRINT: phase=30,status=3002 → phase=40,status=4001")
        void completePrint_shouldAdvanceToPostProcessing() {
            // PRINTING(3002) 在 PRINT 阶段允许 COMPLETE_PRINT
            testOrder.setPhase(30);
            testOrder.setStatus(3002);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.COMPLETE_PRINT, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(40, result.getTargetPhase());
            assertEquals(3003, result.getTargetStatus()); // PRINT_COMPLETED（过渡）
            assertEquals(4001, result.getInitialStatus()); // POST_PROCESSING
            assertEquals(4001, result.getFinalStatus());
        }
    }

    // ==================== 质检合格推进阶段测试 ====================

    @Nested
    @DisplayName("QC_PASS 推进阶段")
    class QcPassTests {

        @Test
        @DisplayName("QC_PASS: phase=50,status=5001 → phase=60,status=6001")
        void qcPass_shouldAdvanceToWarehouse() {
            // QC_IN_PROGRESS(5001) 在 QC 阶段允许 QC_PASS
            testOrder.setPhase(50);
            testOrder.setStatus(5001);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.QC_PASS, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(60, result.getTargetPhase());
            assertEquals(5002, result.getTargetStatus()); // QC_PASSED（过渡）
            assertEquals(6001, result.getInitialStatus()); // WAREHOUSE_IN
            assertEquals(6001, result.getFinalStatus());
        }
    }

    // ==================== 入库完成测试 ====================

    @Nested
    @DisplayName("COMPLETE_WAREHOUSE_IN 入库完成")
    class CompleteWarehouseInTests {

        @Test
        @DisplayName("COMPLETE_WAREHOUSE_IN: phase=60,status=6001 → phase=80,status=8001")
        void completeWarehouseIn_shouldAdvanceToCompleted() {
            // WAREHOUSE_IN(6001) 在 WAREHOUSE 阶段允许 COMPLETE_WAREHOUSE_IN
            testOrder.setPhase(60);
            testOrder.setStatus(6001);
            testOrder.setNeedsPhysicalDelivery(1);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.COMPLETE_WAREHOUSE_IN, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(80, result.getTargetPhase());
            assertEquals(6002, result.getTargetStatus()); // WAREHOUSED（过渡）
            assertEquals(8001, result.getInitialStatus()); // COMPLETED
            assertEquals(8001, result.getFinalStatus());
        }
    }

    // ==================== 客户确认测试 ====================

    @Nested
    @DisplayName("USER_CONFIRM 客户确认")
    class UserConfirmTests {

        @Test
        @DisplayName("USER_CONFIRM: phase=70,status=7001 → phase=80,status=8001")
        void userConfirm_shouldAdvanceToCompleted() {
            // AWAITING_CONFIRM(7001) 在 CONFIRM 阶段允许 USER_CONFIRM
            testOrder.setPhase(70);
            testOrder.setStatus(7001);
            testOrder.setNeedsPhysicalDelivery(0);
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(Collections.emptyList());

            TransitionResult result = flowStateMachineService.executeTransition(
                    1L, FlowActionEnum.USER_CONFIRM, testOperator);

            assertTrue(result.isPhaseChanged());
            assertEquals(80, result.getTargetPhase());
            assertEquals(8001, result.getTargetStatus()); // COMPLETED
            assertEquals(8001, result.getInitialStatus());
            assertEquals(8001, result.getFinalStatus());
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
            testOrder.setPhase(10);
            testOrder.setStatus(1002);
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
        @DisplayName("驳回次数达边界值（历史9次+本次=10次）→ 抛出 ORDER_EXCESSIVE_AUDIT_REJECT")
        void auditRejectAtBoundary_shouldNotThrow() {
            // 模拟历史中已有9次驳回，本次执行后刚好达到上限（10次），应抛出异常
            testOrder.setPhase(10);
            testOrder.setStatus(1002);
            testOrder.setNeedsPhysicalDelivery(1);
            java.util.List<String> historyWith9Rejects = Collections.nCopies(9, "DATA_AUDIT_REJECT");
            when(flowOrderService.getById(1L)).thenReturn(testOrder);
            when(flowStatusHistoryService.listActionCodesByOrderId(1L)).thenReturn(historyWith9Rejects);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> flowStateMachineService.executeTransition(
                            1L, FlowActionEnum.DATA_AUDIT_REJECT, testOperator)
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT.getCode(), ex.getCode());
        }
    }

    // ==================== 非法动作测试 ====================

    @Nested
    @DisplayName("非法动作")
    class InvalidActionTests {

        @Test
        @DisplayName("在不允许的状态下执行动作 → 抛出 ORDER_STATUS_TRANSITION_ERROR")
        void invalidAction_shouldThrow() {
            // DRAFT(1001) 不允许 DATA_AUDIT_PASS（允许 SUBMIT_ORDER）
            testOrder.setPhase(10);
            testOrder.setStatus(1001);
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
            testOrder.setStatus(1001);
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
