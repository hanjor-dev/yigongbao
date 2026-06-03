package com.yigongbao.flow.rules;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 阶段内状态转换规则
 * 定义每个阶段内的状态流转关系
 *
 * 【核心改进】：所有转换规则集中定义，清晰易维护
 * 新增/删除状态只需修改此文件
 *
 * 【订单分类逻辑】：
 * - 订单类型（orderType）：仅区分医疗器械/非医疗器械，用于法规相关判断
 * - 是否需要实体交付（needsPhysicalDelivery）：用于流程分支判断
 *   - needsPhysicalDelivery = 1（需要实体交付）：走完整的生产流程（打印→后处理→质检→仓储）
 *   - needsPhysicalDelivery = 0（不需要实体交付）：跳过生产相关阶段，直接到确认阶段
 *
 * 【方法设计说明】：
 * - getAvailableActions: 实例方法，以 phase 为外层 switch，status 为内层判断
 * - isValidStatusTransition: 实例方法，校验状态转换是否合法
 * - getAllowedNextStatuses: 实例方法，获取当前状态允许的下一状态集合
 * - getValidStatusesForPhase: 实例方法，获取阶段的有效状态列表
 * - getTargetStatus: 实例方法，根据动作获取目标状态，移除 status/10 反推逻辑
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Slf4j
@Component
public class FlowStatusTransitionRules {

    /**
     * 状态转换规则
     * Key: (阶段, 当前状态) → Value: 可跳转的目标状态集合
     */
    private static final Map<StatusKey, Set<FlowStatusEnum>> STATUS_TRANSITIONS;

    static {
        Map<StatusKey, Set<FlowStatusEnum>> transitions = new HashMap<>();

        // ==================== 订单阶段状态转换（1010-1090）====================
        transitions.put(statusKey(FlowPhaseEnum.ORDER, FlowStatusEnum.DRAFT),
                Set.of(FlowStatusEnum.PENDING_DATA_AUDIT));

        transitions.put(statusKey(FlowPhaseEnum.ORDER, FlowStatusEnum.PENDING_DATA_AUDIT),
                Set.of(FlowStatusEnum.DATA_AUDIT_PASSED, FlowStatusEnum.DATA_AUDIT_REJECTED));

        transitions.put(statusKey(FlowPhaseEnum.ORDER, FlowStatusEnum.DATA_AUDIT_PASSED),
                Set.of(FlowStatusEnum.PENDING_DATA_AUDIT)); // 撤回

        transitions.put(statusKey(FlowPhaseEnum.ORDER, FlowStatusEnum.DATA_AUDIT_REJECTED),
                Set.of(FlowStatusEnum.PENDING_DATA_AUDIT)); // 重新提交

        // ==================== 设计阶段状态转换（2010-2090）====================
        // PENDING_DESIGN(2010) → DESIGN_IN_PROGRESS(2020)（设计师开始设计）
        transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.PENDING_DESIGN),
                Set.of(FlowStatusEnum.DESIGN_IN_PROGRESS));

        // DESIGN_IN_PROGRESS(2020) → DESIGN_COMPLETED(2030)（提交设计）
        transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_IN_PROGRESS),
                Set.of(FlowStatusEnum.DESIGN_COMPLETED));

        // DESIGN_COMPLETED 可跨阶段流转
        transitions.put(statusKey(FlowPhaseEnum.DESIGN, FlowStatusEnum.DESIGN_COMPLETED),
                Set.of(FlowStatusEnum.PENDING_PRINT, FlowStatusEnum.AWAITING_CONFIRM));

        // ==================== 打印阶段状态转换（3010-3090）====================
        transitions.put(statusKey(FlowPhaseEnum.PRINT, FlowStatusEnum.PENDING_PRINT),
                Set.of(FlowStatusEnum.PRINTING));

        transitions.put(statusKey(FlowPhaseEnum.PRINT, FlowStatusEnum.PRINTING),
                Set.of(FlowStatusEnum.PRINT_COMPLETED));

        // ==================== 后处理阶段状态转换（4010-4090）====================
        transitions.put(statusKey(FlowPhaseEnum.POST_PROCESSING, FlowStatusEnum.POST_PROCESSING),
                Set.of(FlowStatusEnum.QC_IN_PROGRESS));

        // ==================== 质检阶段状态转换（5010-5090）====================
        transitions.put(statusKey(FlowPhaseEnum.QC, FlowStatusEnum.QC_IN_PROGRESS),
                Set.of(FlowStatusEnum.QC_PASSED, FlowStatusEnum.QC_FAILED, FlowStatusEnum.PENDING_PRINT));

        transitions.put(statusKey(FlowPhaseEnum.QC, FlowStatusEnum.QC_PASSED),
                Set.of(FlowStatusEnum.WAREHOUSE_IN));

        transitions.put(statusKey(FlowPhaseEnum.QC, FlowStatusEnum.QC_FAILED),
                Set.of(FlowStatusEnum.REWORK));

        transitions.put(statusKey(FlowPhaseEnum.QC, FlowStatusEnum.REWORK),
                Set.of(FlowStatusEnum.QC_IN_PROGRESS));

        // ==================== 仓储阶段状态转换（6010-6090）====================
        transitions.put(statusKey(FlowPhaseEnum.WAREHOUSE, FlowStatusEnum.WAREHOUSE_IN),
                Set.of(FlowStatusEnum.WAREHOUSED));

        transitions.put(statusKey(FlowPhaseEnum.WAREHOUSE, FlowStatusEnum.WAREHOUSED),
                Set.of(FlowStatusEnum.COMPLETED));

        // ==================== 确认阶段状态转换（7010-7090，服务订单专用）====================
        transitions.put(statusKey(FlowPhaseEnum.CONFIRM, FlowStatusEnum.AWAITING_CONFIRM),
                Set.of(FlowStatusEnum.COMPLETED));

        STATUS_TRANSITIONS = Map.copyOf(transitions);
    }

    // ==================== 可执行动作查询 ====================

    /**
     * 获取当前可执行的动作列表
     * 【重构】以 phase 为外层 switch，status 为内层判断，增强健壮性
     *
     * @param currentStatus 当前状态值
     * @param phase 当前阶段值
     * @param needsPhysicalDelivery 是否需要实体交付（0-不需要，1-需要）
     * @return 可执行的动作列表
     */
    public List<FlowActionEnum> getAvailableActions(Integer currentStatus, Integer phase, Integer needsPhysicalDelivery) {
        if (currentStatus == null || phase == null) {
            return List.of();
        }
        FlowStatusEnum status = FlowStatusEnum.getByValue(currentStatus);
        FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
        if (status == null || phaseEnum == null) {
            return List.of();
        }

        boolean needsProduction = needsPhysicalDelivery == null || needsPhysicalDelivery == 1;

        // 以阶段为外层 switch，精确区分不同阶段中相同状态码的含义
        List<FlowActionEnum> actions = switch (phaseEnum) {
            case ORDER -> switch (status) {
                case DRAFT -> List.of(FlowActionEnum.SUBMIT_ORDER);
                case PENDING_DATA_AUDIT -> List.of(FlowActionEnum.DATA_AUDIT_PASS, FlowActionEnum.DATA_AUDIT_REJECT);
                case DATA_AUDIT_PASSED -> List.of(FlowActionEnum.WITHDRAW);
                case DATA_AUDIT_REJECTED -> List.of(FlowActionEnum.RESUBMIT);
                default -> List.of();
            };

            case DESIGN -> switch (status) {
                case PENDING_DESIGN -> List.of(FlowActionEnum.START_DESIGN);
                case DESIGN_IN_PROGRESS -> List.of(FlowActionEnum.SUBMIT_DESIGN);
                case DESIGN_COMPLETED -> List.of();
                default -> List.of();
            };

            case PRINT -> switch (status) {
                case PENDING_PRINT -> needsProduction ? List.of(FlowActionEnum.START_PRINT) : List.of();
                case PRINTING -> List.of(FlowActionEnum.COMPLETE_PRINT);
                // PRINT_COMPLETED 为过渡状态，不会出现在 phase=PRINT 的订单中（自动推进）
                default -> List.of();
            };

            case POST_PROCESSING -> switch (status) {
                case POST_PROCESSING -> needsProduction
                        ? List.of(FlowActionEnum.COMPLETE_POST_PROCESSING)
                        : List.of();
                default -> List.of();
            };

            case QC -> switch (status) {
                case QC_IN_PROGRESS -> needsProduction
                        ? List.of(FlowActionEnum.QC_PASS, FlowActionEnum.QC_FAIL)
                        : List.of();
                case QC_FAILED -> List.of(FlowActionEnum.REWORK);
                case REWORK -> List.of(FlowActionEnum.REWORK_COMPLETE);
                // QC_PASSED 为过渡状态，不会出现在 phase=QC 的订单中（自动推进）
                default -> List.of();
            };

            case WAREHOUSE -> switch (status) {
                case WAREHOUSE_IN -> needsProduction
                        ? List.of(FlowActionEnum.COMPLETE_WAREHOUSE_IN)
                        : List.of();
                // WAREHOUSED 为过渡状态，不会出现在 phase=WAREHOUSE 的订单中（自动推进）
                default -> List.of();
            };

            case CONFIRM -> switch (status) {
                case AWAITING_CONFIRM -> !needsProduction
                        ? List.of(FlowActionEnum.USER_CONFIRM)
                        : List.of();
                default -> List.of();
            };

            case COMPLETED -> List.of();
        };

        // 添加全局可用动作：取消（排除终态）
        if (status != FlowStatusEnum.COMPLETED && status != FlowStatusEnum.CANCELLED) {
            List<FlowActionEnum> result = new ArrayList<>(actions);
            result.add(FlowActionEnum.CANCEL);
            return result;
        }

        return actions;
    }

    /**
     * 判断指定动作是否可以执行
     *
     * @param currentStatus 当前状态值
     * @param phase 当前阶段值
     * @param needsPhysicalDelivery 是否需要实体交付
     * @param action 动作枚举
     * @return true-可执行，false-不可执行
     */
    public boolean canExecuteAction(Integer currentStatus, Integer phase, Integer needsPhysicalDelivery, FlowActionEnum action) {
        if (currentStatus == null || action == null) {
            return false;
        }
        return getAvailableActions(currentStatus, phase, needsPhysicalDelivery).contains(action);
    }

    // ==================== 目标状态查询 ====================

    /**
     * 获取动作执行后的目标状态
     * 【重构】移除 status/10 反推逻辑，改为显式 switch 表达式
     *
     * @param currentStatus 当前状态值
     * @param action 动作枚举
     * @return 目标状态值，如果无法执行则返回 null
     */
    public Integer getTargetStatus(Integer currentStatus, FlowActionEnum action) {
        if (currentStatus == null || action == null) {
            return null;
        }

        // 通用动作在任何状态下都有效
        return switch (action) {
            case CANCEL -> FlowStatusEnum.CANCELLED.getValue(); // 废弃订单，全阶段可用
            case COMPLETE -> FlowStatusEnum.COMPLETED.getValue();
            case CREATE -> currentStatus; // 保持当前状态

            // 订单阶段动作
            case SUBMIT_ORDER -> FlowStatusEnum.PENDING_DATA_AUDIT.getValue();
            case DATA_AUDIT_PASS -> FlowStatusEnum.DATA_AUDIT_PASSED.getValue();
            case DATA_AUDIT_REJECT -> FlowStatusEnum.DATA_AUDIT_REJECTED.getValue();
            case WITHDRAW -> FlowStatusEnum.PENDING_DATA_AUDIT.getValue();
            case RESUBMIT -> FlowStatusEnum.PENDING_DATA_AUDIT.getValue();

            // 设计阶段动作
            case START_DESIGN -> FlowStatusEnum.DESIGN_IN_PROGRESS.getValue();
            case SUBMIT_DESIGN -> FlowStatusEnum.DESIGN_COMPLETED.getValue();

            // 打印阶段动作
            case START_PRINT -> FlowStatusEnum.PRINTING.getValue();
            case COMPLETE_PRINT -> FlowStatusEnum.PRINT_COMPLETED.getValue(); // 过渡状态，自动推进

            // 后处理动作
            case COMPLETE_POST_PROCESSING -> FlowStatusEnum.QC_IN_PROGRESS.getValue(); // 自动进入质检

            // 质检阶段动作
            case QC_PASS -> FlowStatusEnum.QC_PASSED.getValue(); // 自动进入入库
            case QC_FAIL -> FlowStatusEnum.QC_FAILED.getValue();
            case REWORK -> FlowStatusEnum.REWORK.getValue();
            case REWORK_COMPLETE -> FlowStatusEnum.QC_IN_PROGRESS.getValue();
            case REWORK_TO_PRINT -> FlowStatusEnum.PENDING_PRINT.getValue();

            // 仓储阶段动作
            case COMPLETE_WAREHOUSE_IN -> FlowStatusEnum.WAREHOUSED.getValue();

            // 确认阶段动作
            case USER_CONFIRM -> FlowStatusEnum.COMPLETED.getValue();

            default -> null;
        };
    }

    // ==================== 静态规则校验（跨阶段）====================

    /**
     * 判断状态转换是否合法（改为实例方法）
     *
     * @param phase 当前阶段
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @param needsPhysicalDelivery 是否需要实体交付
     * @return 是否合法
     */
    public boolean isValidStatusTransition(
            FlowPhaseEnum phase,
            FlowStatusEnum currentStatus,
            FlowStatusEnum targetStatus,
            Integer needsPhysicalDelivery) {

        Set<FlowStatusEnum> allowedStatuses = STATUS_TRANSITIONS.get(
                statusKey(phase, currentStatus));

        if (allowedStatuses == null || allowedStatuses.isEmpty()) {
            return false;
        }

        return allowedStatuses.contains(targetStatus);
    }

    /**
     * 获取当前状态允许的下一状态集合（改为实例方法）
     *
     * @param phase 当前阶段
     * @param currentStatus 当前状态
     * @param needsPhysicalDelivery 是否需要实体交付
     * @return 允许的目标状态集合
     */
    public Set<FlowStatusEnum> getAllowedNextStatuses(
            FlowPhaseEnum phase,
            FlowStatusEnum currentStatus,
            Integer needsPhysicalDelivery) {

        Set<FlowStatusEnum> allowed = new HashSet<>();

        Set<FlowStatusEnum> baseAllowed = STATUS_TRANSITIONS.get(
                statusKey(phase, currentStatus));
        if (baseAllowed != null) {
            allowed.addAll(baseAllowed);
        }

        return Collections.unmodifiableSet(allowed);
    }

    /**
     * 获取阶段的有效状态列表（改为实例方法）
     *
     * @param phase 阶段
     * @param needsPhysicalDelivery 是否需要实体交付
     * @return 有效状态集合
     */
    public Set<FlowStatusEnum> getValidStatusesForPhase(
            FlowPhaseEnum phase, Integer needsPhysicalDelivery) {

        boolean needsProduction = needsPhysicalDelivery == null || needsPhysicalDelivery == 1;

        return switch (phase) {
            case ORDER -> Set.of(FlowStatusEnum.DRAFT, FlowStatusEnum.PENDING_DATA_AUDIT,
                    FlowStatusEnum.DATA_AUDIT_PASSED, FlowStatusEnum.DATA_AUDIT_REJECTED);

            case DESIGN -> Set.of(FlowStatusEnum.PENDING_DESIGN, FlowStatusEnum.DESIGN_IN_PROGRESS,
                    FlowStatusEnum.DESIGN_COMPLETED, FlowStatusEnum.DESIGN_REVIEWING,
                    FlowStatusEnum.DESIGN_REVIEW_REJECTED);

            case PRINT -> needsProduction
                    ? Set.of(FlowStatusEnum.PENDING_PRINT, FlowStatusEnum.PRINTING, FlowStatusEnum.PRINT_COMPLETED)
                    : Set.of();

            case POST_PROCESSING -> needsProduction
                    ? Set.of(FlowStatusEnum.POST_PROCESSING)
                    : Set.of();

            case QC -> needsProduction
                    ? Set.of(FlowStatusEnum.QC_IN_PROGRESS, FlowStatusEnum.QC_FAILED, FlowStatusEnum.REWORK)
                    : Set.of();

            case WAREHOUSE -> needsProduction
                    ? Set.of(FlowStatusEnum.WAREHOUSE_IN, FlowStatusEnum.WAREHOUSED)
                    : Set.of();

            case CONFIRM -> !needsProduction
                    ? Set.of(FlowStatusEnum.AWAITING_CONFIRM)
                    : Set.of();

            case COMPLETED -> Set.of(FlowStatusEnum.COMPLETED);
        };
    }

    /**
     * 获取状态转换的说明
     *
     * @param fromStatus 起始状态
     * @param action 执行的动作
     * @return 转换说明
     */
    public String getTransitionDescription(Integer fromStatus, FlowActionEnum action) {
        FlowStatusEnum from = FlowStatusEnum.getByValue(fromStatus);
        if (from == null || action == null) {
            return "状态转换失败";
        }
        Integer targetStatus = getTargetStatus(fromStatus, action);
        if (targetStatus == null) {
            return "状态转换失败";
        }
        FlowStatusEnum to = FlowStatusEnum.getByValue(targetStatus);
        if (to == null) {
            return "状态转换失败";
        }
        return String.format("%s → %s", from.getName(), to.getName());
    }

    // ==================== 私有方法 ====================

    /**
     * 创建状态键（用于Map的key）
     */
    private static StatusKey statusKey(FlowPhaseEnum phase, FlowStatusEnum status) {
        return new StatusKey(phase, status);
    }

    /**
     * 状态键内部类
     */
    private record StatusKey(FlowPhaseEnum phase, FlowStatusEnum status) {}
}
