package com.yigongbao.flow.rules;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.rules.FlowPhaseTransitionRules.PhaseAndStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowPhaseTransitionRules 单元测试
 * 测试阶段流转规则的核心逻辑
 *
 * @author hanjor
 * @date 2026-04-02
 */
@DisplayName("FlowPhaseTransitionRules 单元测试")
class FlowPhaseTransitionRulesTest {

    private FlowPhaseTransitionRules rules;

    @BeforeEach
    void setUp() {
        rules = new FlowPhaseTransitionRules();
    }

    // ==================== canTransition 测试 ====================

    @Nested
    @DisplayName("canTransition")
    class CanTransitionTests {

        @Test
        @DisplayName("ORDER → DESIGN（合法）")
        void order_to_design_shouldReturn_true() {
            assertTrue(rules.canTransition(10, 20));
        }

        @Test
        @DisplayName("DESIGN → PRINT（合法）")
        void design_to_print_shouldReturn_true() {
            assertTrue(rules.canTransition(20, 30));
        }

        @Test
        @DisplayName("PRINT → POST_PROCESSING（合法）")
        void print_to_postProcessing_shouldReturn_true() {
            assertTrue(rules.canTransition(30, 40));
        }

        @Test
        @DisplayName("POST_PROCESSING → QC（合法）")
        void postProcessing_to_qc_shouldReturn_true() {
            assertTrue(rules.canTransition(40, 50));
        }

        @Test
        @DisplayName("QC → WAREHOUSE（合法）")
        void qc_to_warehouse_shouldReturn_true() {
            assertTrue(rules.canTransition(50, 60));
        }

        @Test
        @DisplayName("WAREHOUSE → COMPLETED（合法）")
        void warehouse_to_completed_shouldReturn_true() {
            assertTrue(rules.canTransition(60, 80));
        }

        @Test
        @DisplayName("COMPLETED → 任意阶段（非法，已完成不可跳转）")
        void completed_to_any_shouldReturn_false() {
            assertFalse(rules.canTransition(80, 10));
            assertFalse(rules.canTransition(80, 20));
        }

        @Test
        @DisplayName("ORDER → PRINT（非法，跨阶段跳转）")
        void order_to_print_shouldReturn_false() {
            assertFalse(rules.canTransition(10, 30));
        }

        @Test
        @DisplayName("逆向跳转（非法，排除QC→PRINT返工场景）")
        void reverse_transition_shouldReturn_false() {
            assertFalse(rules.canTransition(30, 20));
            // QC(50) → PRINT(30) 是允许的（返工场景）
            assertTrue(rules.canTransition(50, 30));
        }

        @Test
        @DisplayName("null 参数 → 返回 false")
        void null_params_shouldReturn_false() {
            assertFalse(rules.canTransition(null, 20));
            assertFalse(rules.canTransition(10, null));
            assertFalse(rules.canTransition(null, null));
        }

        @Test
        @DisplayName("非法阶段值 → 返回 false")
        void invalid_phase_shouldReturn_false() {
            assertFalse(rules.canTransition(99, 20));
            assertFalse(rules.canTransition(10, 99));
        }
    }

    // ==================== getAvailableNextPhases 测试 ====================

    @Nested
    @DisplayName("getAvailableNextPhases")
    class GetAvailableNextPhasesTests {

        @Test
        @DisplayName("ORDER → [DESIGN]")
        void order_shouldReturn_design() {
            List<Integer> nextPhases = rules.getAvailableNextPhases(10);
            assertEquals(List.of(20), nextPhases);
        }

        @Test
        @DisplayName("DESIGN → [PRINT, COMPLETED]")
        void design_shouldReturn_printAndCompleted() {
            List<Integer> nextPhases = rules.getAvailableNextPhases(20);
            assertEquals(2, nextPhases.size());
            assertTrue(nextPhases.contains(30));
            assertTrue(nextPhases.contains(80));
        }

        @Test
        @DisplayName("PRINT → [POST_PROCESSING]")
        void print_shouldReturn_postProcessing() {
            List<Integer> nextPhases = rules.getAvailableNextPhases(30);
            assertEquals(List.of(40), nextPhases);
        }

        @Test
        @DisplayName("COMPLETED → []（无后续阶段）")
        void completed_shouldReturn_empty() {
            List<Integer> nextPhases = rules.getAvailableNextPhases(80);
            assertTrue(nextPhases.isEmpty());
        }

        @Test
        @DisplayName("null → 返回空列表")
        void null_shouldReturn_empty() {
            assertTrue(rules.getAvailableNextPhases(null).isEmpty());
        }
    }

    // ==================== getNextPhase 测试 ====================

    @Nested
    @DisplayName("getNextPhase")
    class GetNextPhaseTests {

        @Test
        @DisplayName("ORDER → DESIGN")
        void order_shouldReturn_design() {
            assertEquals(FlowPhaseEnum.DESIGN,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.ORDER, 1));
        }

        @Test
        @DisplayName("DESIGN + needsPhysicalDelivery=1 → PRINT")
        void design_needDelivery_shouldReturn_print() {
            assertEquals(FlowPhaseEnum.PRINT,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.DESIGN, 1));
        }

        @Test
        @DisplayName("DESIGN + needsPhysicalDelivery=0 → COMPLETED")
        void design_noDelivery_shouldReturn_completed() {
            assertEquals(FlowPhaseEnum.COMPLETED,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.DESIGN, 0));
        }

        @Test
        @DisplayName("DESIGN + needsPhysicalDelivery=null → 视为需要实体交付")
        void design_nullDelivery_shouldTreatAsDelivery() {
            assertEquals(FlowPhaseEnum.PRINT,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.DESIGN, null));
        }

        @Test
        @DisplayName("PRINT → POST_PROCESSING")
        void print_shouldReturn_postProcessing() {
            assertEquals(FlowPhaseEnum.POST_PROCESSING,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.PRINT, 1));
        }

        @Test
        @DisplayName("POST_PROCESSING → QC")
        void postProcessing_shouldReturn_qc() {
            assertEquals(FlowPhaseEnum.QC,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.POST_PROCESSING, 1));
        }

        @Test
        @DisplayName("QC → WAREHOUSE")
        void qc_shouldReturn_warehouse() {
            assertEquals(FlowPhaseEnum.WAREHOUSE,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.QC, 1));
        }

        @Test
        @DisplayName("WAREHOUSE → COMPLETED")
        void warehouse_shouldReturn_completed() {
            assertEquals(FlowPhaseEnum.COMPLETED,
                    FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.WAREHOUSE, 1));
        }

        @Test
        @DisplayName("COMPLETED → null（已完成无后续阶段）")
        void completed_shouldReturn_null() {
            assertNull(FlowPhaseTransitionRules.getNextPhase(FlowPhaseEnum.COMPLETED, 1));
        }
    }

    // ==================== decideNextPhaseAndStatus 测试（核心）====================

    @Nested
    @DisplayName("decideNextPhaseAndStatus（核心）")
    class DecideNextPhaseAndStatusTests {

        @Test
        @DisplayName("DATA_AUDIT_PASSED(1030) → DESIGN + PENDING_DESIGN(2010)")
        void dataAuditPassed_shouldAdvanceToDesign() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.ORDER,
                    FlowStatusEnum.DATA_AUDIT_PASSED,
                    FlowActionEnum.DATA_AUDIT_PASS,
                    1, null);
            assertEquals(FlowPhaseEnum.DESIGN, result.phase());
            assertEquals(FlowStatusEnum.PENDING_DESIGN, result.initialStatus());
        }

        @Test
        @DisplayName("DESIGN_COMPLETED(2030) + needsPhysicalDelivery=1 → 不自动推进")
        void designCompleted_needDelivery_shouldStayInDesignPhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.DESIGN,
                    FlowStatusEnum.DESIGN_COMPLETED,
                    FlowActionEnum.COMPLETE_DESIGN,
                    1, null);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("DESIGN_COMPLETED(2030) + needsPhysicalDelivery=0 → 保持在 DESIGN 阶段，不推进")
        void designCompleted_noDelivery_shouldStayInDesignPhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.DESIGN,
                    FlowStatusEnum.DESIGN_COMPLETED,
                    FlowActionEnum.COMPLETE_DESIGN,
                    0, null);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("PRINT_COMPLETED(3030) 医疗器械 → 不自动推进")
        void printCompleted_medical_shouldStayInPrintPhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.PRINT,
                    FlowStatusEnum.PRINT_COMPLETED,
                    FlowActionEnum.COMPLETE_PRINT,
                    1, 1);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("PRINT_COMPLETED(3030) 非医疗器械 → 不自动推进")
        void printCompleted_nonMedical_shouldStayInPrintPhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.PRINT,
                    FlowStatusEnum.PRINT_COMPLETED,
                    FlowActionEnum.COMPLETE_PRINT,
                    1, 2);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("PACKING(5050) → 不推进阶段")
        void packing_shouldStayInQcPhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.QC,
                    FlowStatusEnum.PACKING,
                    FlowActionEnum.QC_PASS,
                    1, null);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("WAREHOUSED(6020) → 不自动推进")
        void warehoused_shouldStayInWarehousePhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.WAREHOUSE,
                    FlowStatusEnum.WAREHOUSED,
                    FlowActionEnum.COMPLETE_WAREHOUSE_IN,
                    1, null);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("REWORK → null, null（不推进阶段）")
        void rework_shouldNotChangePhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.QC,
                    FlowStatusEnum.REWORK,
                    FlowActionEnum.REWORK,
                    1, null);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }

        @Test
        @DisplayName("DATA_AUDIT_REJECT → null, null（不推进阶段）")
        void auditReject_shouldNotChangePhase() {
            PhaseAndStatus result = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    FlowPhaseEnum.ORDER,
                    FlowStatusEnum.DATA_AUDIT_REJECTED,
                    FlowActionEnum.DATA_AUDIT_REJECT,
                    1, null);
            assertNull(result.phase());
            assertNull(result.initialStatus());
        }
    }

    // ==================== isInvisibleStatus 测试 ====================

    @Nested
    @DisplayName("isInvisibleStatus")
    class IsInvisibleStatusTests {

        @Test
        @DisplayName("所有状态 → false（可见状态）")
        void allStatuses_shouldBeVisible() {
            assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.DRAFT));
            assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.PENDING_DATA_AUDIT));
            assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.DESIGN_IN_PROGRESS));
            assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.DESIGN_COMPLETED));
            assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.PRINTING));
            assertFalse(FlowPhaseTransitionRules.isInvisibleStatus(FlowStatusEnum.COMPLETED));
        }
    }

    // ==================== isPhaseChangeAction 测试 ====================

    @Nested
    @DisplayName("isPhaseChangeAction")
    class IsPhaseChangeActionTests {

        @Test
        @DisplayName("会触发阶段推进的动作 → true")
        void phaseChangeActions_shouldReturn_true() {
            assertTrue(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.DATA_AUDIT_PASS));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.COMPLETE_DESIGN));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.COMPLETE_PRINT));
            assertTrue(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.COMPLETE_POST_PROCESSING));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.QC_PASS));
            assertTrue(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.COMPLETE_WAREHOUSE_IN));
        }

        @Test
        @DisplayName("不会触发阶段推进的动作 → false")
        void nonPhaseChangeActions_shouldReturn_false() {
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.SUBMIT_ORDER));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.DATA_AUDIT_REJECT));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.WITHDRAW));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.START_DESIGN));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.START_PRINT));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.QC_FAIL));
            assertFalse(FlowPhaseTransitionRules.isPhaseChangeAction(FlowActionEnum.REWORK));
        }
    }

    // ==================== isValidPhaseTransition 测试 ====================

    @Nested
    @DisplayName("isValidPhaseTransition")
    class IsValidPhaseTransitionTests {

        @Test
        @DisplayName("COMPLETED → 任意阶段（非法）")
        void completed_to_any_shouldReturn_false() {
            assertFalse(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.COMPLETED, FlowPhaseEnum.ORDER, 1));
        }

        @Test
        @DisplayName("needsPhysicalDelivery=0 时不允许进入 PRINT/POST_PROCESSING/QC/WAREHOUSE")
        void noDelivery_shouldBlockProductionPhases() {
            assertFalse(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.DESIGN, FlowPhaseEnum.PRINT, 0));
            assertFalse(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.POST_PROCESSING, FlowPhaseEnum.QC, 0));
            assertFalse(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.QC, FlowPhaseEnum.WAREHOUSE, 0));
        }

        @Test
        @DisplayName("needsPhysicalDelivery=1 时允许进入所有生产阶段")
        void needDelivery_shouldAllowProductionPhases() {
            assertTrue(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.DESIGN, FlowPhaseEnum.PRINT, 1));
            assertTrue(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.POST_PROCESSING, FlowPhaseEnum.QC, 1));
            assertTrue(FlowPhaseTransitionRules.isValidPhaseTransition(
                    FlowPhaseEnum.QC, FlowPhaseEnum.WAREHOUSE, 1));
        }
    }
}
