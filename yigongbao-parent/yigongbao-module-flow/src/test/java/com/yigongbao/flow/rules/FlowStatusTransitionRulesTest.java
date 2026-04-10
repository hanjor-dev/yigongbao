package com.yigongbao.flow.rules;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowStatusTransitionRules 单元测试
 * 测试状态转换规则的核心逻辑
 *
 * @author hanjor
 * @date 2026-04-02
 */
@DisplayName("FlowStatusTransitionRules 单元测试")
class FlowStatusTransitionRulesTest {

    private FlowStatusTransitionRules rules;

    @BeforeEach
    void setUp() {
        rules = new FlowStatusTransitionRules();
    }

    // ==================== getAvailableActions 测试 ====================

    @Nested
    @DisplayName("getAvailableActions")
    class GetAvailableActionsTests {

        // ==================== ORDER 阶段测试 ====================

        @Test
        @DisplayName("phase=ORDER, status=DRAFT(1001) → 返回 [SUBMIT_ORDER]")
        void order_phase_draft_shouldReturn_submitOrder() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1001, 10, 1);
            assertEquals(List.of(FlowActionEnum.SUBMIT_ORDER), actions);
        }

        @Test
        @DisplayName("phase=ORDER, status=PENDING_DATA_AUDIT(1002) → 返回 [DATA_AUDIT_PASS, DATA_AUDIT_REJECT]")
        void order_phase_pendingAudit_shouldReturn_auditActions() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1002, 10, 1);
            assertEquals(2, actions.size());
            assertTrue(actions.contains(FlowActionEnum.DATA_AUDIT_PASS));
            assertTrue(actions.contains(FlowActionEnum.DATA_AUDIT_REJECT));
        }

        @Test
        @DisplayName("phase=ORDER, status=DATA_AUDIT_PASSED(1003) → 返回 [WITHDRAW]")
        void order_phase_auditPassed_shouldReturn_withdraw() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1003, 10, 1);
            assertEquals(List.of(FlowActionEnum.WITHDRAW), actions);
        }

        @Test
        @DisplayName("phase=ORDER, status=DATA_AUDIT_REJECTED(1004) → 返回 [RESUBMIT]")
        void order_phase_auditRejected_shouldReturn_resubmit() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1004, 10, 1);
            assertEquals(List.of(FlowActionEnum.RESUBMIT), actions);
        }

        // ==================== DESIGN 阶段测试 ====================

        @Test
        @DisplayName("phase=DESIGN, status=PENDING_DESIGN(2001) → 返回 [START_DESIGN]")
        void design_phase_pendingDesign_shouldReturn_startDesign() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2001, 20, 1);
            assertEquals(List.of(FlowActionEnum.START_DESIGN), actions);
        }

        @Test
        @DisplayName("phase=DESIGN, status=DESIGN_IN_PROGRESS(2002) → 返回 [SUBMIT_DESIGN]")
        void design_phase_designInProgress_shouldReturn_submitDesign() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2002, 20, 1);
            assertEquals(List.of(FlowActionEnum.SUBMIT_DESIGN), actions);
        }

        @Test
        @DisplayName("phase=DESIGN, status=DESIGN_COMPLETED(2003) → 返回 [SUBMIT_DESIGN]")
        void design_phase_designCompleted_shouldReturn_submitDesign() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2003, 20, 1);
            assertEquals(List.of(FlowActionEnum.SUBMIT_DESIGN), actions);
        }

        @Test
        @DisplayName("phase=DESIGN, status=DESIGN_REVIEWING(2004) → 返回 [DESIGN_REVIEW_PASS, DESIGN_REVIEW_REJECT]")
        void design_phase_reviewing_shouldReturn_reviewActions() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2004, 20, 1);
            assertEquals(2, actions.size());
            assertTrue(actions.contains(FlowActionEnum.DESIGN_REVIEW_PASS));
            assertTrue(actions.contains(FlowActionEnum.DESIGN_REVIEW_REJECT));
        }

        @Test
        @DisplayName("phase=DESIGN, status=DESIGN_REVIEW_REJECTED(2006) → 返回 [START_DESIGN]")
        void design_phase_reviewRejected_shouldReturn_startDesign() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2006, 20, 1);
            assertEquals(List.of(FlowActionEnum.START_DESIGN), actions);
        }

        // ==================== PRINT 阶段测试 ====================

        @Test
        @DisplayName("phase=PRINT, status=PENDING_PRINT(3001), needsPhysicalDelivery=1 → 返回 [START_PRINT]")
        void print_phase_pendingPrint_needDelivery_shouldReturn_startPrint() {
            List<FlowActionEnum> actions = rules.getAvailableActions(3001, 30, 1);
            assertEquals(List.of(FlowActionEnum.START_PRINT), actions);
        }

        @Test
        @DisplayName("phase=PRINT, status=PENDING_PRINT(3001), needsPhysicalDelivery=0 → 返回空列表")
        void print_phase_pendingPrint_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(3001, 30, 0);
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("phase=PRINT, status=PRINTING(3002) → 返回 [COMPLETE_PRINT]")
        void print_phase_printing_shouldReturn_completePrint() {
            List<FlowActionEnum> actions = rules.getAvailableActions(3002, 30, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_PRINT), actions);
        }

        // ==================== POST_PROCESSING 阶段测试 ====================

        @Test
        @DisplayName("phase=POST_PROCESSING, status=POST_PROCESSING(4001), needsPhysicalDelivery=1 → 返回 [COMPLETE_POST_PROCESSING]")
        void postProcessing_phase_needDelivery_shouldReturn_completePostProcessing() {
            List<FlowActionEnum> actions = rules.getAvailableActions(4001, 40, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_POST_PROCESSING), actions);
        }

        @Test
        @DisplayName("phase=POST_PROCESSING, status=POST_PROCESSING(4001), needsPhysicalDelivery=0 → 返回空列表")
        void postProcessing_phase_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(4001, 40, 0);
            assertTrue(actions.isEmpty());
        }

        // ==================== QC 阶段测试 ====================

        @Test
        @DisplayName("phase=QC, status=QC_IN_PROGRESS(5001), needsPhysicalDelivery=1 → 返回 [QC_PASS, QC_FAIL]")
        void qc_phase_inProgress_needDelivery_shouldReturn_qcActions() {
            List<FlowActionEnum> actions = rules.getAvailableActions(5001, 50, 1);
            assertEquals(2, actions.size());
            assertTrue(actions.contains(FlowActionEnum.QC_PASS));
            assertTrue(actions.contains(FlowActionEnum.QC_FAIL));
        }

        @Test
        @DisplayName("phase=QC, status=QC_IN_PROGRESS(5001), needsPhysicalDelivery=0 → 返回空列表")
        void qc_phase_inProgress_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(5001, 50, 0);
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("phase=QC, status=QC_FAILED(5003) → 返回 [REWORK]")
        void qc_phase_failed_shouldReturn_rework() {
            List<FlowActionEnum> actions = rules.getAvailableActions(5003, 50, 1);
            assertEquals(List.of(FlowActionEnum.REWORK), actions);
        }

        // ==================== WAREHOUSE 阶段测试 ====================

        @Test
        @DisplayName("phase=WAREHOUSE, status=WAREHOUSE_IN(6001), needsPhysicalDelivery=1 → 返回 [COMPLETE_WAREHOUSE_IN]")
        void warehouse_phase_in_needDelivery_shouldReturn_completeWarehouseIn() {
            List<FlowActionEnum> actions = rules.getAvailableActions(6001, 60, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_WAREHOUSE_IN), actions);
        }

        @Test
        @DisplayName("phase=WAREHOUSE, status=WAREHOUSE_IN(6001), needsPhysicalDelivery=0 → 返回空列表")
        void warehouse_phase_in_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(6001, 60, 0);
            assertTrue(actions.isEmpty());
        }

        // ==================== CONFIRM 阶段测试 ====================

        @Test
        @DisplayName("phase=CONFIRM, status=AWAITING_CONFIRM(7001), needsPhysicalDelivery=0 → 返回 [USER_CONFIRM]")
        void confirm_phase_awaiting_noDelivery_shouldReturn_userConfirm() {
            List<FlowActionEnum> actions = rules.getAvailableActions(7001, 70, 0);
            assertEquals(List.of(FlowActionEnum.USER_CONFIRM), actions);
        }

        @Test
        @DisplayName("phase=CONFIRM, status=AWAITING_CONFIRM(7001), needsPhysicalDelivery=1 → 返回空列表（需要生产不应进入此阶段）")
        void confirm_phase_awaiting_needDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(7001, 70, 1);
            assertTrue(actions.isEmpty());
        }

        // ==================== COMPLETED 阶段测试 ====================

        @Test
        @DisplayName("phase=COMPLETED, status=COMPLETED(8001) → 返回空列表")
        void completed_phase_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(8001, 80, 1);
            assertTrue(actions.isEmpty());
        }

        // ==================== 边界测试 ====================

        @Test
        @DisplayName("currentStatus=null → 返回空列表")
        void nullStatus_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(null, 10, 1);
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("phase=null → 返回空列表")
        void nullPhase_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1001, null, 1);
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("needsPhysicalDelivery=null → 视为需要实体交付（按1处理）")
        void nullNeedsPhysicalDelivery_shouldTreatAsOne() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1002, 10, null);
            assertEquals(2, actions.size());
            assertTrue(actions.contains(FlowActionEnum.DATA_AUDIT_PASS));
        }

        @Test
        @DisplayName("非法状态值 → 返回空列表")
        void invalidStatus_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(999, 10, 1);
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("非法阶段值 → 返回空列表")
        void invalidPhase_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1001, 99, 1);
            assertTrue(actions.isEmpty());
        }
    }

    // ==================== getTargetStatus 测试 ====================

    @Nested
    @DisplayName("getTargetStatus")
    class GetTargetStatusTests {

        @Test
        @DisplayName("SUBMIT_ORDER → PENDING_DATA_AUDIT(1002)")
        void submitOrder_shouldReturn_pendingAudit() {
            assertEquals(1002, rules.getTargetStatus(1001, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("DATA_AUDIT_PASS → DATA_AUDIT_PASSED(1003)")
        void auditPass_shouldReturn_auditPassed() {
            assertEquals(1003, rules.getTargetStatus(1002, FlowActionEnum.DATA_AUDIT_PASS));
        }

        @Test
        @DisplayName("DATA_AUDIT_REJECT → DATA_AUDIT_REJECTED(1004)")
        void auditReject_shouldReturn_auditRejected() {
            assertEquals(1004, rules.getTargetStatus(1002, FlowActionEnum.DATA_AUDIT_REJECT));
        }

        @Test
        @DisplayName("WITHDRAW → PENDING_DATA_AUDIT(1002)")
        void withdraw_shouldReturn_pendingAudit() {
            assertEquals(1002, rules.getTargetStatus(1003, FlowActionEnum.WITHDRAW));
        }

        @Test
        @DisplayName("RESUBMIT → PENDING_DATA_AUDIT(1002)")
        void resubmit_shouldReturn_pendingAudit() {
            assertEquals(1002, rules.getTargetStatus(1004, FlowActionEnum.RESUBMIT));
        }

        @Test
        @DisplayName("START_DESIGN → DESIGN_IN_PROGRESS(2002)")
        void startDesign_shouldReturn_designInProgress() {
            assertEquals(2002, rules.getTargetStatus(2001, FlowActionEnum.START_DESIGN));
        }

        @Test
        @DisplayName("SUBMIT_DESIGN → DESIGN_REVIEWING(2004)")
        void submitDesign_shouldReturn_designReviewing() {
            assertEquals(2004, rules.getTargetStatus(2002, FlowActionEnum.SUBMIT_DESIGN));
        }

        @Test
        @DisplayName("DESIGN_REVIEW_PASS → DESIGN_REVIEW_PASSED(2005)（不可见状态）")
        void designReviewPass_shouldReturn_designReviewPassed() {
            assertEquals(2005, rules.getTargetStatus(2004, FlowActionEnum.DESIGN_REVIEW_PASS));
        }

        @Test
        @DisplayName("DESIGN_REVIEW_REJECT → DESIGN_REVIEW_REJECTED(2006)")
        void designReviewReject_shouldReturn_designReviewRejected() {
            assertEquals(2006, rules.getTargetStatus(2004, FlowActionEnum.DESIGN_REVIEW_REJECT));
        }

        @Test
        @DisplayName("START_PRINT → PRINTING(3002)")
        void startPrint_shouldReturn_printing() {
            assertEquals(3002, rules.getTargetStatus(3001, FlowActionEnum.START_PRINT));
        }

        @Test
        @DisplayName("COMPLETE_PRINT → PRINT_COMPLETED(3003)（过渡状态）")
        void completePrint_shouldReturn_printCompleted() {
            assertEquals(3003, rules.getTargetStatus(3002, FlowActionEnum.COMPLETE_PRINT));
        }

        @Test
        @DisplayName("COMPLETE_POST_PROCESSING → QC_IN_PROGRESS(5001)")
        void completePostProcessing_shouldReturn_qcInProgress() {
            assertEquals(5001, rules.getTargetStatus(4001, FlowActionEnum.COMPLETE_POST_PROCESSING));
        }

        @Test
        @DisplayName("QC_PASS → QC_PASSED(5002)")
        void qcPass_shouldReturn_qcPassed() {
            assertEquals(5002, rules.getTargetStatus(5001, FlowActionEnum.QC_PASS));
        }

        @Test
        @DisplayName("QC_FAIL → QC_FAILED(5003)")
        void qcFail_shouldReturn_qcFailed() {
            assertEquals(5003, rules.getTargetStatus(5001, FlowActionEnum.QC_FAIL));
        }

        @Test
        @DisplayName("REWORK → REWORK(5004)")
        void rework_shouldReturn_rework() {
            assertEquals(5004, rules.getTargetStatus(5003, FlowActionEnum.REWORK));
        }

        @Test
        @DisplayName("COMPLETE_WAREHOUSE_IN → WAREHOUSED(6002)")
        void completeWarehouseIn_shouldReturn_warehoused() {
            assertEquals(6002, rules.getTargetStatus(6001, FlowActionEnum.COMPLETE_WAREHOUSE_IN));
        }

        @Test
        @DisplayName("USER_CONFIRM → COMPLETED(8001)")
        void userConfirm_shouldReturn_completed() {
            assertEquals(8001, rules.getTargetStatus(7001, FlowActionEnum.USER_CONFIRM));
        }

        @Test
        @DisplayName("CANCEL → DATA_AUDIT_REJECTED(1004)")
        void cancel_shouldReturn_auditRejected() {
            assertEquals(1004, rules.getTargetStatus(1001, FlowActionEnum.CANCEL));
        }

        @Test
        @DisplayName("COMPLETE → COMPLETED(8001)")
        void complete_shouldReturn_completed() {
            assertEquals(8001, rules.getTargetStatus(6002, FlowActionEnum.COMPLETE));
        }

        @Test
        @DisplayName("CREATE → 保持当前状态")
        void create_shouldReturn_currentStatus() {
            assertEquals(1002, rules.getTargetStatus(1002, FlowActionEnum.CREATE));
        }

        @Test
        @DisplayName("currentStatus=null → 返回 null")
        void nullCurrentStatus_shouldReturn_null() {
            assertNull(rules.getTargetStatus(null, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("action=null → 返回 null")
        void nullAction_shouldReturn_null() {
            assertNull(rules.getTargetStatus(1001, null));
        }
    }

    // ==================== canExecuteAction 测试 ====================

    @Nested
    @DisplayName("canExecuteAction")
    class CanExecuteActionTests {

        @Test
        @DisplayName("有效动作 → 返回 true")
        void validAction_shouldReturn_true() {
            assertTrue(rules.canExecuteAction(1001, 10, 1, FlowActionEnum.SUBMIT_ORDER));
            assertTrue(rules.canExecuteAction(1002, 10, 1, FlowActionEnum.DATA_AUDIT_PASS));
            assertTrue(rules.canExecuteAction(1002, 10, 1, FlowActionEnum.DATA_AUDIT_REJECT));
        }

        @Test
        @DisplayName("无效动作 → 返回 false")
        void invalidAction_shouldReturn_false() {
            assertFalse(rules.canExecuteAction(1001, 10, 1, FlowActionEnum.DATA_AUDIT_PASS));
            assertFalse(rules.canExecuteAction(1002, 10, 1, FlowActionEnum.SUBMIT_ORDER));
            assertFalse(rules.canExecuteAction(8001, 80, 1, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("currentStatus=null → 返回 false")
        void nullStatus_shouldReturn_false() {
            assertFalse(rules.canExecuteAction(null, 10, 1, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("action=null → 返回 false")
        void nullAction_shouldReturn_false() {
            assertFalse(rules.canExecuteAction(1001, 10, 1, null));
        }
    }

    // ==================== isValidStatusTransition 测试 ====================

    @Nested
    @DisplayName("isValidStatusTransition")
    class IsValidStatusTransitionTests {

        @Test
        @DisplayName("有效转换 → 返回 true")
        void validTransition_shouldReturn_true() {
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.ORDER, FlowStatusEnum.DRAFT,
                    FlowStatusEnum.PENDING_DATA_AUDIT, 1));
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.ORDER, FlowStatusEnum.PENDING_DATA_AUDIT,
                    FlowStatusEnum.DATA_AUDIT_PASSED, 1));
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.ORDER, FlowStatusEnum.PENDING_DATA_AUDIT,
                    FlowStatusEnum.DATA_AUDIT_REJECTED, 1));
        }

        @Test
        @DisplayName("无效转换 → 返回 false")
        void invalidTransition_shouldReturn_false() {
            assertFalse(rules.isValidStatusTransition(
                    FlowPhaseEnum.ORDER, FlowStatusEnum.DRAFT,
                    FlowStatusEnum.DATA_AUDIT_PASSED, 1));
            assertFalse(rules.isValidStatusTransition(
                    FlowPhaseEnum.ORDER, FlowStatusEnum.DRAFT,
                    FlowStatusEnum.DESIGN_IN_PROGRESS, 1));
        }

        @Test
        @DisplayName("DESIGN_REVIEW_PASSED + needsPhysicalDelivery=0 → 允许跳转到 AWAITING_CONFIRM")
        void designReviewPassed_noDelivery_shouldAllow_confirm() {
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_REVIEW_PASSED,
                    FlowStatusEnum.AWAITING_CONFIRM, 0));
        }

        @Test
        @DisplayName("DESIGN_REVIEW_PASSED + needsPhysicalDelivery=1 → 允许跳转到 PENDING_PRINT")
        void designReviewPassed_needDelivery_shouldAllow_print() {
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_REVIEW_PASSED,
                    FlowStatusEnum.PENDING_PRINT, 1));
        }
    }

    // ==================== getValidStatusesForPhase 测试 ====================

    @Nested
    @DisplayName("getValidStatusesForPhase")
    class GetValidStatusesForPhaseTests {

        @Test
        @DisplayName("ORDER 阶段可见状态")
        void order_phase_visibleStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.ORDER, 1);
            assertEquals(4, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.DRAFT));
            assertTrue(statuses.contains(FlowStatusEnum.PENDING_DATA_AUDIT));
            assertTrue(statuses.contains(FlowStatusEnum.DATA_AUDIT_PASSED));
            assertTrue(statuses.contains(FlowStatusEnum.DATA_AUDIT_REJECTED));
        }

        @Test
        @DisplayName("DESIGN 阶段可见状态（不包含不可见的 DESIGN_REVIEW_PASSED）")
        void design_phase_visibleStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.DESIGN, 1);
            assertEquals(5, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.PENDING_DESIGN));
            assertTrue(statuses.contains(FlowStatusEnum.DESIGN_IN_PROGRESS));
            assertTrue(statuses.contains(FlowStatusEnum.DESIGN_COMPLETED));
            assertTrue(statuses.contains(FlowStatusEnum.DESIGN_REVIEWING));
            assertTrue(statuses.contains(FlowStatusEnum.DESIGN_REVIEW_REJECTED));
            assertFalse(statuses.contains(FlowStatusEnum.DESIGN_REVIEW_PASSED));
        }

        @Test
        @DisplayName("PRINT 阶段 needsPhysicalDelivery=1 → 有打印相关状态")
        void print_phase_needDelivery_hasStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.PRINT, 1);
            assertEquals(3, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.PENDING_PRINT));
            assertTrue(statuses.contains(FlowStatusEnum.PRINTING));
            assertTrue(statuses.contains(FlowStatusEnum.PRINT_COMPLETED));
        }

        @Test
        @DisplayName("PRINT 阶段 needsPhysicalDelivery=0 → 空集合")
        void print_phase_noDelivery_emptyStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.PRINT, 0);
            assertTrue(statuses.isEmpty());
        }

        @Test
        @DisplayName("POST_PROCESSING 阶段 needsPhysicalDelivery=1 → 有状态")
        void postProcessing_phase_needDelivery_hasStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.POST_PROCESSING, 1);
            assertEquals(1, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.POST_PROCESSING));
        }

        @Test
        @DisplayName("POST_PROCESSING 阶段 needsPhysicalDelivery=0 → 空集合")
        void postProcessing_phase_noDelivery_emptyStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.POST_PROCESSING, 0);
            assertTrue(statuses.isEmpty());
        }

        @Test
        @DisplayName("QC 阶段 needsPhysicalDelivery=1 → 有质检相关状态")
        void qc_phase_needDelivery_hasStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.QC, 1);
            assertEquals(3, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.QC_IN_PROGRESS));
            assertTrue(statuses.contains(FlowStatusEnum.QC_FAILED));
            assertTrue(statuses.contains(FlowStatusEnum.REWORK));
        }

        @Test
        @DisplayName("QC 阶段 needsPhysicalDelivery=0 → 空集合")
        void qc_phase_noDelivery_emptyStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.QC, 0);
            assertTrue(statuses.isEmpty());
        }

        @Test
        @DisplayName("WAREHOUSE 阶段 needsPhysicalDelivery=1 → 有仓储状态")
        void warehouse_phase_needDelivery_hasStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.WAREHOUSE, 1);
            assertEquals(2, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.WAREHOUSE_IN));
            assertTrue(statuses.contains(FlowStatusEnum.WAREHOUSED));
        }

        @Test
        @DisplayName("WAREHOUSE 阶段 needsPhysicalDelivery=0 → 空集合")
        void warehouse_phase_noDelivery_emptyStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.WAREHOUSE, 0);
            assertTrue(statuses.isEmpty());
        }

        @Test
        @DisplayName("CONFIRM 阶段 needsPhysicalDelivery=0 → 有确认状态")
        void confirm_phase_noDelivery_hasStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.CONFIRM, 0);
            assertEquals(1, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.AWAITING_CONFIRM));
        }

        @Test
        @DisplayName("CONFIRM 阶段 needsPhysicalDelivery=1 → 空集合")
        void confirm_phase_needDelivery_emptyStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.CONFIRM, 1);
            assertTrue(statuses.isEmpty());
        }

        @Test
        @DisplayName("COMPLETED 阶段 → 仅 COMPLETED 状态")
        void completed_phase_onlyCompletedStatus() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.COMPLETED, 1);
            assertEquals(1, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.COMPLETED));
        }
    }

    // ==================== getTransitionDescription 测试 ====================

    @Nested
    @DisplayName("getTransitionDescription")
    class GetTransitionDescriptionTests {

        @Test
        @DisplayName("返回状态转换描述")
        void shouldReturnTransitionDescription() {
            String desc = rules.getTransitionDescription(1001, FlowActionEnum.SUBMIT_ORDER);
            assertTrue(desc.contains("草稿"));
            assertTrue(desc.contains("数据待审核"));
        }

        @Test
        @DisplayName("null 参数 → 返回失败描述")
        void nullParams_shouldReturnFailureDescription() {
            assertEquals("状态转换失败", rules.getTransitionDescription(null, FlowActionEnum.SUBMIT_ORDER));
            assertEquals("状态转换失败", rules.getTransitionDescription(1001, null));
        }

        @Test
        @DisplayName("任意合法动作都有对应目标状态，返回格式化的转换描述")
        void anyValidAction_shouldReturnFormattedDescription() {
            // CANCEL → DATA_AUDIT_REJECTED(1004)，有对应状态，返回 "草稿 → 数据审核不通过"
            String desc = rules.getTransitionDescription(1001, FlowActionEnum.CANCEL);
            assertTrue(desc.contains("草稿"));
            assertTrue(desc.contains("数据审核不通过"));
        }
    }
}
