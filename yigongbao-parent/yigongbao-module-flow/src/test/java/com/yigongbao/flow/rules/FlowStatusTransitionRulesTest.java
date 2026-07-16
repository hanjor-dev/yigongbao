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
        @DisplayName("phase=ORDER, status=DRAFT(1010) → 返回 [SUBMIT_ORDER]")
        void order_phase_draft_shouldReturn_submitOrder() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1010, 10, 1);
            assertEquals(List.of(FlowActionEnum.SUBMIT_ORDER, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=ORDER, status=PENDING_DATA_AUDIT(1020) → 返回 [DATA_AUDIT_PASS, DATA_AUDIT_REJECT]")
        void order_phase_pendingAudit_shouldReturn_auditActions() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1020, 10, 1);
            assertEquals(3, actions.size());
            assertTrue(actions.contains(FlowActionEnum.DATA_AUDIT_PASS));
            assertTrue(actions.contains(FlowActionEnum.DATA_AUDIT_REJECT));
            assertTrue(actions.contains(FlowActionEnum.CANCEL));
        }

        @Test
        @DisplayName("phase=ORDER, status=DATA_AUDIT_PASSED(1030) → 返回 [WITHDRAW]")
        void order_phase_auditPassed_shouldReturn_withdraw() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1030, 10, 1);
            assertEquals(List.of(FlowActionEnum.WITHDRAW, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=ORDER, status=DATA_AUDIT_REJECTED(1040) → 返回 [RESUBMIT]")
        void order_phase_auditRejected_shouldReturn_resubmit() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1040, 10, 1);
            assertEquals(List.of(FlowActionEnum.RESUBMIT, FlowActionEnum.CANCEL), actions);
        }

        // ==================== DESIGN 阶段测试 ====================

        @Test
        @DisplayName("phase=DESIGN, status=PENDING_DESIGN(2010) → 返回 [START_DESIGN]")
        void design_phase_pendingDesign_shouldReturn_startDesign() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2010, 20, 1);
            assertEquals(List.of(FlowActionEnum.START_DESIGN, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=DESIGN, status=DESIGN_IN_PROGRESS(2020) → 返回 [COMPLETE_DESIGN]")
        void design_phase_designInProgress_shouldReturn_completeDesign() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2020, 20, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_DESIGN, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=DESIGN, status=DESIGN_COMPLETED(2030) → 返回 [DOWNLOAD_DATA_PACKAGE]")
        void design_phase_designCompleted_shouldReturn_downloadDataPackage() {
            List<FlowActionEnum> actions = rules.getAvailableActions(2030, 20, 1);
            assertEquals(List.of(FlowActionEnum.DOWNLOAD_DATA_PACKAGE, FlowActionEnum.CANCEL), actions);
        }

        // ==================== PRINT 阶段测试 ====================

        @Test
        @DisplayName("phase=PRINT, status=PENDING_PRINT(3010), needsPhysicalDelivery=1 → 返回 [START_PRINT]")
        void print_phase_pendingPrint_needDelivery_shouldReturn_startPrint() {
            List<FlowActionEnum> actions = rules.getAvailableActions(3010, 30, 1);
            assertEquals(List.of(FlowActionEnum.START_PRINT, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=PRINT, status=PENDING_PRINT(3010), needsPhysicalDelivery=0 → 返回空列表")
        void print_phase_pendingPrint_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(3010, 30, 0);
            assertEquals(List.of(FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=PRINT, status=PRINTING(3020) → 返回 [COMPLETE_PRINT]")
        void print_phase_printing_shouldReturn_completePrint() {
            List<FlowActionEnum> actions = rules.getAvailableActions(3020, 30, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_PRINT, FlowActionEnum.CANCEL), actions);
        }

        // ==================== POST_PROCESSING 阶段测试 ====================

        @Test
        @DisplayName("phase=POST_PROCESSING, status=POST_PROCESSING(4010), needsPhysicalDelivery=1 → 返回 [COMPLETE_POST_PROCESSING]")
        void postProcessing_phase_needDelivery_shouldReturn_completePostProcessing() {
            List<FlowActionEnum> actions = rules.getAvailableActions(4010, 40, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_POST_PROCESSING, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=POST_PROCESSING, status=POST_PROCESSING(4010), needsPhysicalDelivery=0 → 返回空列表")
        void postProcessing_phase_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(4010, 40, 0);
            assertEquals(List.of(FlowActionEnum.CANCEL), actions);
        }

        // ==================== QC 阶段测试 ====================

        @Test
        @DisplayName("phase=QC, status=QC_IN_PROGRESS(5010), needsPhysicalDelivery=1 → 返回 [QC_PASS, QC_FAIL]")
        void qc_phase_inProgress_needDelivery_shouldReturn_qcActions() {
            List<FlowActionEnum> actions = rules.getAvailableActions(5010, 50, 1);
            assertEquals(3, actions.size());
            assertTrue(actions.contains(FlowActionEnum.QC_PASS));
            assertTrue(actions.contains(FlowActionEnum.QC_FAIL));
            assertTrue(actions.contains(FlowActionEnum.CANCEL));
        }

        @Test
        @DisplayName("phase=QC, status=QC_IN_PROGRESS(5010), needsPhysicalDelivery=0 → 返回空列表")
        void qc_phase_inProgress_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(5010, 50, 0);
            assertEquals(List.of(FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=QC, status=QC_FAILED(5030) → 返回 [REWORK]")
        void qc_phase_failed_shouldReturn_rework() {
            List<FlowActionEnum> actions = rules.getAvailableActions(5030, 50, 1);
            assertEquals(List.of(FlowActionEnum.REWORK, FlowActionEnum.CANCEL), actions);
        }

        // ==================== WAREHOUSE 阶段测试 ====================

        @Test
        @DisplayName("phase=WAREHOUSE, status=WAREHOUSE_IN(6010), needsPhysicalDelivery=1 → 返回 [COMPLETE_WAREHOUSE_IN]")
        void warehouse_phase_in_needDelivery_shouldReturn_completeWarehouseIn() {
            List<FlowActionEnum> actions = rules.getAvailableActions(6010, 60, 1);
            assertEquals(List.of(FlowActionEnum.COMPLETE_WAREHOUSE_IN, FlowActionEnum.CANCEL), actions);
        }

        @Test
        @DisplayName("phase=WAREHOUSE, status=WAREHOUSE_IN(6010), needsPhysicalDelivery=0 → 返回空列表")
        void warehouse_phase_in_noDelivery_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(6010, 60, 0);
            assertEquals(List.of(FlowActionEnum.CANCEL), actions);
        }

        // ==================== COMPLETED 阶段测试 ====================

        @Test
        @DisplayName("phase=COMPLETED, status=COMPLETED(8010) → 返回空列表")
        void completed_phase_shouldReturn_empty() {
            List<FlowActionEnum> actions = rules.getAvailableActions(8010, 80, 1);
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
            List<FlowActionEnum> actions = rules.getAvailableActions(1010, null, 1);
            assertTrue(actions.isEmpty());
        }

        @Test
        @DisplayName("needsPhysicalDelivery=null → 视为需要实体交付（按1处理）")
        void nullNeedsPhysicalDelivery_shouldTreatAsOne() {
            List<FlowActionEnum> actions = rules.getAvailableActions(1020, 10, null);
            assertEquals(3, actions.size());
            assertTrue(actions.contains(FlowActionEnum.DATA_AUDIT_PASS));
            assertTrue(actions.contains(FlowActionEnum.CANCEL));
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
            List<FlowActionEnum> actions = rules.getAvailableActions(1010, 99, 1);
            assertTrue(actions.isEmpty());
        }
    }

    // ==================== getTargetStatus 测试 ====================

    @Nested
    @DisplayName("getTargetStatus")
    class GetTargetStatusTests {

        @Test
        @DisplayName("SUBMIT_ORDER → PENDING_DATA_AUDIT(1020)")
        void submitOrder_shouldReturn_pendingAudit() {
            assertEquals(1020, rules.getTargetStatus(1010, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("DATA_AUDIT_PASS → DATA_AUDIT_PASSED(1030)")
        void auditPass_shouldReturn_auditPassed() {
            assertEquals(1030, rules.getTargetStatus(1020, FlowActionEnum.DATA_AUDIT_PASS));
        }

        @Test
        @DisplayName("DATA_AUDIT_REJECT → DATA_AUDIT_REJECTED(1040)")
        void auditReject_shouldReturn_auditRejected() {
            assertEquals(1040, rules.getTargetStatus(1020, FlowActionEnum.DATA_AUDIT_REJECT));
        }

        @Test
        @DisplayName("WITHDRAW → PENDING_DATA_AUDIT(1020)")
        void withdraw_shouldReturn_pendingAudit() {
            assertEquals(1020, rules.getTargetStatus(1030, FlowActionEnum.WITHDRAW));
        }

        @Test
        @DisplayName("RESUBMIT → PENDING_DATA_AUDIT(1020)")
        void resubmit_shouldReturn_pendingAudit() {
            assertEquals(1020, rules.getTargetStatus(1040, FlowActionEnum.RESUBMIT));
        }

        @Test
        @DisplayName("START_DESIGN → DESIGN_IN_PROGRESS(2020)")
        void startDesign_shouldReturn_designInProgress() {
            assertEquals(2020, rules.getTargetStatus(2010, FlowActionEnum.START_DESIGN));
        }

        @Test
        @DisplayName("COMPLETE_DESIGN → DESIGN_COMPLETED(2030)")
        void completeDesign_shouldReturn_designCompleted() {
            assertEquals(2030, rules.getTargetStatus(2020, FlowActionEnum.COMPLETE_DESIGN));
        }

        @Test
        @DisplayName("START_PRINT → PRINTING(3020)")
        void startPrint_shouldReturn_printing() {
            assertEquals(3020, rules.getTargetStatus(3010, FlowActionEnum.START_PRINT));
        }

        @Test
        @DisplayName("COMPLETE_PRINT → PRINT_COMPLETED(3030)（过渡状态）")
        void completePrint_shouldReturn_printCompleted() {
            assertEquals(3030, rules.getTargetStatus(3020, FlowActionEnum.COMPLETE_PRINT));
        }

        @Test
        @DisplayName("COMPLETE_POST_PROCESSING → QC_IN_PROGRESS(5010)")
        void completePostProcessing_shouldReturn_qcInProgress() {
            assertEquals(5010, rules.getTargetStatus(4010, FlowActionEnum.COMPLETE_POST_PROCESSING));
        }

        @Test
        @DisplayName("QC_PASS → PACKING(5050)")
        void qcPass_shouldReturn_qcPassed() {
            assertEquals(5050, rules.getTargetStatus(5010, FlowActionEnum.QC_PASS));
        }

        @Test
        @DisplayName("QC_FAIL → QC_FAILED(5030)")
        void qcFail_shouldReturn_qcFailed() {
            assertEquals(5030, rules.getTargetStatus(5010, FlowActionEnum.QC_FAIL));
        }

        @Test
        @DisplayName("REWORK → REWORK(5040)")
        void rework_shouldReturn_rework() {
            assertEquals(5040, rules.getTargetStatus(5030, FlowActionEnum.REWORK));
        }

        @Test
        @DisplayName("COMPLETE_WAREHOUSE_IN → WAREHOUSED(6020)")
        void completeWarehouseIn_shouldReturn_warehoused() {
            assertEquals(6020, rules.getTargetStatus(6010, FlowActionEnum.COMPLETE_WAREHOUSE_IN));
        }

        @Test
        @DisplayName("CANCEL → CANCELLED(9010)")
        void cancel_shouldReturn_auditRejected() {
            assertEquals(9010, rules.getTargetStatus(1010, FlowActionEnum.CANCEL));
        }

        @Test
        @DisplayName("COMPLETE → COMPLETED(8010)")
        void complete_shouldReturn_completed() {
            assertEquals(8010, rules.getTargetStatus(6020, FlowActionEnum.COMPLETE));
        }

        @Test
        @DisplayName("CREATE → 保持当前状态")
        void create_shouldReturn_currentStatus() {
            assertEquals(1020, rules.getTargetStatus(1020, FlowActionEnum.CREATE));
        }

        @Test
        @DisplayName("currentStatus=null → 返回 null")
        void nullCurrentStatus_shouldReturn_null() {
            assertNull(rules.getTargetStatus(null, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("action=null → 返回 null")
        void nullAction_shouldReturn_null() {
            assertNull(rules.getTargetStatus(1010, null));
        }
    }

    // ==================== canExecuteAction 测试 ====================

    @Nested
    @DisplayName("canExecuteAction")
    class CanExecuteActionTests {

        @Test
        @DisplayName("有效动作 → 返回 true")
        void validAction_shouldReturn_true() {
            assertTrue(rules.canExecuteAction(1010, 10, 1, FlowActionEnum.SUBMIT_ORDER));
            assertTrue(rules.canExecuteAction(1020, 10, 1, FlowActionEnum.DATA_AUDIT_PASS));
            assertTrue(rules.canExecuteAction(1020, 10, 1, FlowActionEnum.DATA_AUDIT_REJECT));
        }

        @Test
        @DisplayName("无效动作 → 返回 false")
        void invalidAction_shouldReturn_false() {
            assertFalse(rules.canExecuteAction(1010, 10, 1, FlowActionEnum.DATA_AUDIT_PASS));
            assertFalse(rules.canExecuteAction(1020, 10, 1, FlowActionEnum.SUBMIT_ORDER));
            assertFalse(rules.canExecuteAction(8010, 80, 1, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("currentStatus=null → 返回 false")
        void nullStatus_shouldReturn_false() {
            assertFalse(rules.canExecuteAction(null, 10, 1, FlowActionEnum.SUBMIT_ORDER));
        }

        @Test
        @DisplayName("action=null → 返回 false")
        void nullAction_shouldReturn_false() {
            assertFalse(rules.canExecuteAction(1010, 10, 1, null));
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
        @DisplayName("DESIGN_COMPLETED + needsPhysicalDelivery=0 → 允许跳转到 COMPLETED")
        void designCompleted_noDelivery_shouldAllow_completed() {
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED,
                    FlowStatusEnum.COMPLETED, 0));
        }

        @Test
        @DisplayName("DESIGN_COMPLETED + needsPhysicalDelivery=1 → 允许跳转到 PENDING_PRINT")
        void designCompleted_needDelivery_shouldAllow_print() {
            assertTrue(rules.isValidStatusTransition(
                    FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED,
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
        @DisplayName("DESIGN 阶段可见状态")
        void design_phase_visibleStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.DESIGN, 1);
            assertEquals(3, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.PENDING_DESIGN));
            assertTrue(statuses.contains(FlowStatusEnum.DESIGN_IN_PROGRESS));
            assertTrue(statuses.contains(FlowStatusEnum.DESIGN_COMPLETED));
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
            assertEquals(4, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.QC_IN_PROGRESS));
            assertTrue(statuses.contains(FlowStatusEnum.QC_FAILED));
            assertTrue(statuses.contains(FlowStatusEnum.REWORK));
            assertTrue(statuses.contains(FlowStatusEnum.PACKING));
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
            assertEquals(3, statuses.size());
            assertTrue(statuses.contains(FlowStatusEnum.PENDING_WAREHOUSE_IN));
            assertTrue(statuses.contains(FlowStatusEnum.WAREHOUSED));
            assertTrue(statuses.contains(FlowStatusEnum.WAREHOUSE_OUT));
        }

        @Test
        @DisplayName("WAREHOUSE 阶段 needsPhysicalDelivery=0 → 空集合")
        void warehouse_phase_noDelivery_emptyStatuses() {
            Set<FlowStatusEnum> statuses = rules.getValidStatusesForPhase(FlowPhaseEnum.WAREHOUSE, 0);
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
            String desc = rules.getTransitionDescription(1010, FlowActionEnum.SUBMIT_ORDER);
            assertTrue(desc.contains("草稿"));
            assertTrue(desc.contains("数据待审核"));
        }

        @Test
        @DisplayName("null 参数 → 返回失败描述")
        void nullParams_shouldReturnFailureDescription() {
            assertEquals("状态转换失败", rules.getTransitionDescription(null, FlowActionEnum.SUBMIT_ORDER));
            assertEquals("状态转换失败", rules.getTransitionDescription(1010, null));
        }

        @Test
        @DisplayName("任意合法动作都有对应目标状态，返回格式化的转换描述")
        void anyValidAction_shouldReturnFormattedDescription() {
            // CANCEL → CANCELLED(9010)，有对应状态，返回 "草稿 → 已取消"
            String desc = rules.getTransitionDescription(1010, FlowActionEnum.CANCEL);
            assertTrue(desc.contains("草稿"));
            assertTrue(desc.contains("已取消"));
        }
    }
}
