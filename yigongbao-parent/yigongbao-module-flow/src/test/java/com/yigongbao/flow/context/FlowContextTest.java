package com.yigongbao.flow.context;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowContext 单元测试
 * 测试状态机循环次数校验逻辑
 *
 * @author hanjor
 * @date 2026-04-02
 */
@DisplayName("FlowContext 单元测试")
class FlowContextTest {

    // ==================== buildFromHistory 测试 ====================

    @Nested
    @DisplayName("buildFromHistory")
    class BuildFromHistoryTests {

        @Test
        @DisplayName("空历史 → 所有计数为 0")
        void emptyHistory_shouldReturnZeroCounts() {
            FlowContext ctx = FlowContext.buildFromHistory(List.of());
            assertEquals(0, ctx.getAuditRejectCount());
            assertEquals(0, ctx.getReworkCount());
            assertEquals(0, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("null 历史 → 所有计数为 0")
        void nullHistory_shouldReturnZeroCounts() {
            FlowContext ctx = FlowContext.buildFromHistory(null);
            assertEquals(0, ctx.getAuditRejectCount());
            assertEquals(0, ctx.getReworkCount());
            assertEquals(0, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("仅包含 DATA_AUDIT_REJECT → auditRejectCount=1")
        void onlyAuditReject_shouldIncrementAuditRejectCount() {
            List<String> history = List.of(
                    "DATA_AUDIT_REJECT",
                    "DATA_AUDIT_REJECT",
                    "DATA_AUDIT_REJECT"
            );
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertEquals(3, ctx.getAuditRejectCount());
            assertEquals(0, ctx.getReworkCount());
            assertEquals(0, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("仅包含 REWORK → reworkCount=1")
        void onlyRework_shouldIncrementReworkCount() {
            List<String> history = List.of(
                    "REWORK",
                    "REWORK"
            );
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertEquals(0, ctx.getAuditRejectCount());
            assertEquals(2, ctx.getReworkCount());
            assertEquals(0, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("仅包含 DESIGN_REVIEW_REJECT → designRejectCount=1")
        void onlyDesignReviewReject_shouldIncrementDesignRejectCount() {
            List<String> history = List.of(
                    "DESIGN_REVIEW_REJECT",
                    "DESIGN_REVIEW_REJECT",
                    "DESIGN_REVIEW_REJECT"
            );
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertEquals(0, ctx.getAuditRejectCount());
            assertEquals(0, ctx.getReworkCount());
            assertEquals(3, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("混合历史 → 各计数正确累加")
        void mixedHistory_shouldAccumulateCorrectly() {
            List<String> history = List.of(
                    "DATA_AUDIT_REJECT",
                    "DATA_AUDIT_REJECT",
                    "REWORK",
                    "DESIGN_REVIEW_REJECT",
                    "DATA_AUDIT_REJECT"
            );
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertEquals(3, ctx.getAuditRejectCount());
            assertEquals(1, ctx.getReworkCount());
            assertEquals(1, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("包含无关动作 → 忽略，不影响计数")
        void irrelevantActions_shouldBeIgnored() {
            List<String> history = List.of(
                    "SUBMIT_ORDER",
                    "DATA_AUDIT_PASS",
                    "DATA_AUDIT_REJECT",
                    "START_DESIGN",
                    "REWORK"
            );
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertEquals(1, ctx.getAuditRejectCount());
            assertEquals(1, ctx.getReworkCount());
            assertEquals(0, ctx.getDesignRejectCount());
        }
    }

    // ==================== 循环次数校验测试 ====================

    @Nested
    @DisplayName("循环次数校验 validateNoExcessiveLoops")
    class ValidateNoExcessiveLoopsTests {

        @Test
        @DisplayName("驳回次数=10（边界值） → 抛出 ORDER_EXCESSIVE_AUDIT_REJECT")
        void auditRejectCount_atBoundary_shouldNotThrow() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 10; i++) {
                ctx.incrementAuditReject();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("驳回次数=11 → 抛出 ORDER_EXCESSIVE_AUDIT_REJECT")
        void auditRejectCount_exceedBoundary_shouldThrow() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 11; i++) {
                ctx.incrementAuditReject();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("10"));
        }

        @Test
        @DisplayName("返工次数=5（边界值） → 抛出 ORDER_EXCESSIVE_REWORK")
        void reworkCount_atBoundary_shouldNotThrow() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 5; i++) {
                ctx.incrementRework();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_REWORK.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("返工次数=6 → 抛出 ORDER_EXCESSIVE_REWORK")
        void reworkCount_exceedBoundary_shouldThrow() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 6; i++) {
                ctx.incrementRework();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_REWORK.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("5"));
        }

        @Test
        @DisplayName("设计审核驳回次数=5（边界值） → 抛出 ORDER_EXCESSIVE_DESIGN_REJECT")
        void designRejectCount_atBoundary_shouldNotThrow() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 5; i++) {
                ctx.incrementDesignReject();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_DESIGN_REJECT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("设计审核驳回次数=6 → 抛出 ORDER_EXCESSIVE_DESIGN_REJECT")
        void designRejectCount_exceedBoundary_shouldThrow() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 6; i++) {
                ctx.incrementDesignReject();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_DESIGN_REJECT.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("5"));
        }

        @Test
        @DisplayName("驳回和返工同时超限 → 抛出驳回异常（第一个满足条件的）")
        void auditAndReworkBothExceed_shouldThrowFirst() {
            FlowContext ctx = new FlowContext();
            for (int i = 0; i < 11; i++) {
                ctx.incrementAuditReject();
            }
            for (int i = 0; i < 6; i++) {
                ctx.incrementRework();
            }
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            // 校验顺序决定先抛出驳回异常
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("所有计数为 0 → 不抛出异常")
        void allZero_shouldNotThrow() {
            FlowContext ctx = new FlowContext();
            assertDoesNotThrow(ctx::validateNoExcessiveLoops);
        }
    }

    // ==================== 增量方法测试 ====================

    @Nested
    @DisplayName("增量方法")
    class IncrementMethodTests {

        @Test
        @DisplayName("incrementAuditReject 正确递增")
        void incrementAuditReject_shouldIncrement() {
            FlowContext ctx = new FlowContext();
            assertEquals(0, ctx.getAuditRejectCount());
            ctx.incrementAuditReject();
            assertEquals(1, ctx.getAuditRejectCount());
            ctx.incrementAuditReject();
            assertEquals(2, ctx.getAuditRejectCount());
        }

        @Test
        @DisplayName("incrementRework 正确递增")
        void incrementRework_shouldIncrement() {
            FlowContext ctx = new FlowContext();
            assertEquals(0, ctx.getReworkCount());
            ctx.incrementRework();
            assertEquals(1, ctx.getReworkCount());
        }

        @Test
        @DisplayName("incrementDesignReject 正确递增")
        void incrementDesignReject_shouldIncrement() {
            FlowContext ctx = new FlowContext();
            assertEquals(0, ctx.getDesignRejectCount());
            ctx.incrementDesignReject();
            assertEquals(1, ctx.getDesignRejectCount());
        }

        @Test
        @DisplayName("三种增量互不影响")
        void threeIncrements_shouldNotAffectEachOther() {
            FlowContext ctx = new FlowContext();
            ctx.incrementAuditReject();
            ctx.incrementAuditReject();
            ctx.incrementRework();
            ctx.incrementDesignReject();
            assertEquals(2, ctx.getAuditRejectCount());
            assertEquals(1, ctx.getReworkCount());
            assertEquals(1, ctx.getDesignRejectCount());
        }
    }

    // ==================== 与 buildFromHistory 联合测试 ====================

    @Nested
    @DisplayName("buildFromHistory + validateNoExcessiveLoops 联合测试")
    class BuildAndValidateTests {

        @Test
        @DisplayName("从历史重建上下文后校验超限")
        void rebuildFromHistory_shouldValidateCorrectly() {
            // 构造包含11次驳回的假历史
            List<String> history = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                history.add("DATA_AUDIT_REJECT");
            }
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertEquals(11, ctx.getAuditRejectCount());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    ctx::validateNoExcessiveLoops
            );
            assertEquals(ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("从历史重建上下文后校验通过（9次驳回，未达上限）")
        void rebuildFromHistory_shouldPassValidation() {
            // 构造包含9次驳回的假历史（未达上限）
            List<String> history = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                history.add("DATA_AUDIT_REJECT");
            }
            FlowContext ctx = FlowContext.buildFromHistory(history);
            assertDoesNotThrow(ctx::validateNoExcessiveLoops);
        }
    }
}
