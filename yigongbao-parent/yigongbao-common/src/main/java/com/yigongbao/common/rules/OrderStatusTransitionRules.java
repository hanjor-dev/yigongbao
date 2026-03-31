package com.yigongbao.common.rules;

import com.yigongbao.common.enums.order.OrderActionEnum;
import com.yigongbao.common.enums.order.OrderPhaseEnum;
import com.yigongbao.common.enums.order.OrderStatusEnum;
import com.yigongbao.common.enums.order.OrderTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单阶段内状态转换规则
 * 定义每个阶段内的状态流转关系
 *
 * 【核心改进】：所有转换规则集中定义，清晰易维护
 * 新增/删除状态只需修改此文件
 *
 * 【设计审核通过的不可见状态】：
 * - DESIGN_REVIEW_PASSED(24) 为不可见状态
 * - 审核通过后系统自动将订单推进到下一阶段
 * - order_flow_status_history 表中仍记录完整的审核通过动作，便于追溯
 * - 前端不显示"设计审核通过"这个状态
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Slf4j
@Component
public class OrderStatusTransitionRules {

    /**
     * 状态转换规则
     * Key: (阶段, 当前状态) → Value: 可跳转的目标状态集合
     */
    private static final Map<StatusKey, Set<OrderStatusEnum>> STATUS_TRANSITIONS;

    static {
        Map<StatusKey, Set<OrderStatusEnum>> transitions = new HashMap<>();

        // ==================== 订单阶段状态转换（10-19）====================
        transitions.put(statusKey(OrderPhaseEnum.ORDER, OrderStatusEnum.DRAFT),
                Set.of(OrderStatusEnum.PENDING_DATA_AUDIT));

        transitions.put(statusKey(OrderPhaseEnum.ORDER, OrderStatusEnum.PENDING_DATA_AUDIT),
                Set.of(OrderStatusEnum.DATA_AUDIT_PASSED, OrderStatusEnum.DATA_AUDIT_REJECTED));

        transitions.put(statusKey(OrderPhaseEnum.ORDER, OrderStatusEnum.DATA_AUDIT_PASSED),
                Set.of(OrderStatusEnum.PENDING_DATA_AUDIT)); // 撤回

        transitions.put(statusKey(OrderPhaseEnum.ORDER, OrderStatusEnum.DATA_AUDIT_REJECTED),
                Set.of(OrderStatusEnum.PENDING_DATA_AUDIT)); // 重新提交

        // ==================== 设计阶段状态转换（20-29）====================
        transitions.put(statusKey(OrderPhaseEnum.DESIGN, OrderStatusEnum.DESIGNING),
                Set.of(OrderStatusEnum.DESIGN_COMPLETED));

        transitions.put(statusKey(OrderPhaseEnum.DESIGN, OrderStatusEnum.DESIGN_COMPLETED),
                Set.of(OrderStatusEnum.DESIGN_REVIEWING));

        transitions.put(statusKey(OrderPhaseEnum.DESIGN, OrderStatusEnum.DESIGN_REVIEWING),
                Set.of(OrderStatusEnum.DESIGN_REVIEW_PASSED, OrderStatusEnum.DESIGN_REVIEW_REJECTED));

        transitions.put(statusKey(OrderPhaseEnum.DESIGN, OrderStatusEnum.DESIGN_REVIEW_REJECTED),
                Set.of(OrderStatusEnum.DESIGNING));

        // ==================== 打印阶段状态转换（30-39）====================
        transitions.put(statusKey(OrderPhaseEnum.PRINT, OrderStatusEnum.PENDING_PRINT),
                Set.of(OrderStatusEnum.PRINTING));

        transitions.put(statusKey(OrderPhaseEnum.PRINT, OrderStatusEnum.PRINTING),
                Set.of(OrderStatusEnum.PRINT_COMPLETED));

        // ==================== 后处理阶段状态转换（40-49）====================
        transitions.put(statusKey(OrderPhaseEnum.POST_PROCESSING, OrderStatusEnum.POST_PROCESSING),
                Set.of(OrderStatusEnum.QC_IN_PROGRESS));

        // ==================== 质检阶段状态转换（50-59）====================
        transitions.put(statusKey(OrderPhaseEnum.QC, OrderStatusEnum.QC_IN_PROGRESS),
                Set.of(OrderStatusEnum.QC_PASSED, OrderStatusEnum.QC_FAILED));

        transitions.put(statusKey(OrderPhaseEnum.QC, OrderStatusEnum.QC_PASSED),
                Set.of(OrderStatusEnum.WAREHOUSE_IN));

        transitions.put(statusKey(OrderPhaseEnum.QC, OrderStatusEnum.QC_FAILED),
                Set.of(OrderStatusEnum.REWORK));

        transitions.put(statusKey(OrderPhaseEnum.QC, OrderStatusEnum.REWORK),
                Set.of(OrderStatusEnum.PENDING_PRINT));

        // ==================== 仓储阶段状态转换（60-69）====================
        transitions.put(statusKey(OrderPhaseEnum.WAREHOUSE, OrderStatusEnum.WAREHOUSE_IN),
                Set.of(OrderStatusEnum.WAREHOUSED));

        transitions.put(statusKey(OrderPhaseEnum.WAREHOUSE, OrderStatusEnum.WAREHOUSED),
                Set.of(OrderStatusEnum.COMPLETED));

        // ==================== 确认阶段状态转换（70-79，服务订单专用）====================
        transitions.put(statusKey(OrderPhaseEnum.CONFIRM, OrderStatusEnum.AWAITING_CONFIRM),
                Set.of(OrderStatusEnum.COMPLETED));

        STATUS_TRANSITIONS = Map.copyOf(transitions);
    }

    // ==================== 可执行动作查询 ====================

    /**
     * 获取当前可执行的动作列表
     * 根据当前状态、阶段和订单类型返回可执行的动作
     *
     * @param currentStatus 当前状态值
     * @param phase 当前阶段值
     * @param orderType 订单类型值
     * @return 可执行的动作列表
     */
    public List<OrderActionEnum> getAvailableActions(Integer currentStatus, Integer phase, Integer orderType) {
        if (currentStatus == null || phase == null) {
            return List.of();
        }
        OrderStatusEnum status = OrderStatusEnum.getByValue(currentStatus);
        OrderPhaseEnum phaseEnum = OrderPhaseEnum.getByValue(phase);
        OrderTypeEnum typeEnum = orderType != null ? OrderTypeEnum.getByValue(orderType) : null;
        if (status == null || phaseEnum == null) {
            return List.of();
        }
        return switch (status) {
            // ==================== 订单阶段 ====================
            case DRAFT -> List.of(OrderActionEnum.SUBMIT_ORDER);
            case PENDING_DATA_AUDIT -> List.of(OrderActionEnum.DATA_AUDIT_PASS, OrderActionEnum.DATA_AUDIT_REJECT);
            case DATA_AUDIT_PASSED -> List.of(OrderActionEnum.WITHDRAW);
            case DATA_AUDIT_REJECTED -> List.of(OrderActionEnum.RESUBMIT);

            // ==================== 设计阶段（所有订单类型通用）====================
            case DESIGNING -> List.of(OrderActionEnum.SUBMIT_DESIGN);
            case DESIGN_COMPLETED -> List.of(OrderActionEnum.SUBMIT_DESIGN);
            case DESIGN_REVIEWING -> List.of(OrderActionEnum.DESIGN_REVIEW_PASS, OrderActionEnum.DESIGN_REVIEW_REJECT);
            case DESIGN_REVIEW_REJECTED -> List.of(OrderActionEnum.START_DESIGN);
            // DESIGN_REVIEW_PASSED 为不可见状态，不返回任何动作

            // ==================== 打印阶段（仅医疗器械/非医疗器械）====================
            case PENDING_PRINT -> typeEnum != OrderTypeEnum.SERVICE
                    ? List.of(OrderActionEnum.START_PRINT)
                    : List.of();
            case PRINTING -> List.of(OrderActionEnum.COMPLETE_PRINT);
            // PRINT_COMPLETED 为过渡状态，自动进入后处理

            // ==================== 后处理阶段（仅医疗器械/非医疗器械）====================
            case POST_PROCESSING -> typeEnum != OrderTypeEnum.SERVICE
                    ? List.of(OrderActionEnum.COMPLETE_POST_PROCESSING)
                    : List.of();

            // ==================== 质检阶段（仅医疗器械/非医疗器械）====================
            case QC_IN_PROGRESS -> typeEnum != OrderTypeEnum.SERVICE
                    ? List.of(OrderActionEnum.QC_PASS, OrderActionEnum.QC_FAIL)
                    : List.of();
            case QC_FAILED -> List.of(OrderActionEnum.REWORK);
            // QC_PASSED 质检合格后自动入库，不展示动作
            // REWORK 返工状态等待回到待打印

            // ==================== 仓储阶段（仅医疗器械/非医疗器械）====================
            case WAREHOUSE_IN -> typeEnum != OrderTypeEnum.SERVICE
                    ? List.of(OrderActionEnum.COMPLETE_WAREHOUSE_IN)
                    : List.of();

            // ==================== 确认阶段（仅服务订单）====================
            case AWAITING_CONFIRM -> typeEnum == OrderTypeEnum.SERVICE
                    ? List.of(OrderActionEnum.USER_CONFIRM)
                    : List.of();

            // ==================== 终态 ====================
            case COMPLETED -> List.of();
            default -> List.of();
        };
    }

    /**
     * 判断指定动作是否可以执行
     *
     * @param currentStatus 当前状态值
     * @param phase 当前阶段值
     * @param orderType 订单类型值
     * @param action 动作枚举
     * @return true-可执行，false-不可执行
     */
    public boolean canExecuteAction(Integer currentStatus, Integer phase, Integer orderType, OrderActionEnum action) {
        if (currentStatus == null || action == null) {
            return false;
        }
        return getAvailableActions(currentStatus, phase, orderType).contains(action);
    }

    // ==================== 目标状态查询 ====================

    /**
     * 获取动作执行后的目标状态
     *
     * @param currentStatus 当前状态值
     * @param action 动作枚举
     * @return 目标状态值，如果无法执行则返回 null
     */
    public Integer getTargetStatus(Integer currentStatus, OrderActionEnum action) {
        if (currentStatus == null || action == null) {
            return null;
        }
        OrderStatusEnum status = OrderStatusEnum.getByValue(currentStatus);
        if (status == null) {
            return null;
        }
        return switch (action) {
            // 订单阶段动作
            case SUBMIT_ORDER -> OrderStatusEnum.PENDING_DATA_AUDIT.getValue();
            case DATA_AUDIT_PASS -> OrderStatusEnum.DATA_AUDIT_PASSED.getValue();
            case DATA_AUDIT_REJECT -> OrderStatusEnum.DATA_AUDIT_REJECTED.getValue();
            case WITHDRAW -> OrderStatusEnum.PENDING_DATA_AUDIT.getValue(); // 仅从 DATA_AUDIT_PASSED 可撤回
            case RESUBMIT -> OrderStatusEnum.PENDING_DATA_AUDIT.getValue(); // 重新提交审核

            // 设计阶段动作
            case START_DESIGN -> OrderStatusEnum.DESIGNING.getValue();
            case SUBMIT_DESIGN -> OrderStatusEnum.DESIGN_REVIEWING.getValue();
            case DESIGN_REVIEW_PASS -> OrderStatusEnum.DESIGN_REVIEW_PASSED.getValue(); // 不可见状态，自动推进
            case DESIGN_REVIEW_REJECT -> OrderStatusEnum.DESIGN_REVIEW_REJECTED.getValue();

            // 打印阶段动作
            case START_PRINT -> OrderStatusEnum.PRINTING.getValue();
            case COMPLETE_PRINT -> OrderStatusEnum.PRINT_COMPLETED.getValue(); // 过渡状态，自动进入后处理

            // 后处理动作
            case COMPLETE_POST_PROCESSING -> OrderStatusEnum.QC_IN_PROGRESS.getValue(); // 自动进入质检

            // 质检阶段动作
            case QC_PASS -> OrderStatusEnum.QC_PASSED.getValue(); // 自动进入入库
            case QC_FAIL -> OrderStatusEnum.QC_FAILED.getValue();
            case REWORK -> OrderStatusEnum.PENDING_PRINT.getValue(); // 回到待打印

            // 仓储阶段动作
            case COMPLETE_WAREHOUSE_IN -> OrderStatusEnum.WAREHOUSED.getValue(); // 自动进入已完成

            // 确认阶段动作
            case USER_CONFIRM -> OrderStatusEnum.COMPLETED.getValue(); // 服务订单完成

            // 通用动作
            case COMPLETE -> OrderStatusEnum.COMPLETED.getValue();
            case CANCEL -> OrderStatusEnum.DATA_AUDIT_REJECTED.getValue();
            case CREATE -> status.getValue(); // 创建动作保持当前状态

            default -> null;
        };
    }

    // ==================== 静态规则校验（跨阶段）====================

    /**
     * 判断状态转换是否合法
     *
     * @param phase 当前阶段
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @param orderType 订单类型
     * @return 是否合法
     */
    public static boolean isValidStatusTransition(
            OrderPhaseEnum phase,
            OrderStatusEnum currentStatus,
            OrderStatusEnum targetStatus,
            OrderTypeEnum orderType) {

        Set<OrderStatusEnum> allowedStatuses = STATUS_TRANSITIONS.get(
                statusKey(phase, currentStatus));

        if (allowedStatuses == null || allowedStatuses.isEmpty()) {
            return false;
        }

        // 特殊处理：设计审核通过后的跳转根据订单类型不同（不可见状态）
        if (currentStatus == OrderStatusEnum.DESIGN_REVIEW_PASSED) {
            if (orderType == OrderTypeEnum.SERVICE) {
                return targetStatus == OrderStatusEnum.AWAITING_CONFIRM;
            } else {
                return targetStatus == OrderStatusEnum.PENDING_PRINT;
            }
        }

        return allowedStatuses.contains(targetStatus);
    }

    /**
     * 获取当前状态允许的下一状态集合
     *
     * @param phase 当前阶段
     * @param currentStatus 当前状态
     * @param orderType 订单类型
     * @return 允许的目标状态集合
     */
    public static Set<OrderStatusEnum> getAllowedNextStatuses(
            OrderPhaseEnum phase,
            OrderStatusEnum currentStatus,
            OrderTypeEnum orderType) {

        Set<OrderStatusEnum> allowed = new HashSet<>();

        Set<OrderStatusEnum> baseAllowed = STATUS_TRANSITIONS.get(
                statusKey(phase, currentStatus));
        if (baseAllowed != null) {
            allowed.addAll(baseAllowed);
        }

        // 特殊处理：设计审核通过（不可见状态）
        if (currentStatus == OrderStatusEnum.DESIGN_REVIEW_PASSED) {
            allowed.remove(OrderStatusEnum.DESIGN_REVIEW_PASSED);
            if (orderType == OrderTypeEnum.SERVICE) {
                allowed.add(OrderStatusEnum.AWAITING_CONFIRM);
            } else {
                allowed.add(OrderStatusEnum.PENDING_PRINT);
            }
        }

        return Collections.unmodifiableSet(allowed);
    }

    /**
     * 获取阶段的有效状态列表
     *
     * @param phase 阶段
     * @param orderType 订单类型
     * @return 有效状态集合
     */
    public static Set<OrderStatusEnum> getValidStatusesForPhase(
            OrderPhaseEnum phase, OrderTypeEnum orderType) {

        return switch (phase) {
            case ORDER -> Set.of(OrderStatusEnum.DRAFT, OrderStatusEnum.PENDING_DATA_AUDIT,
                    OrderStatusEnum.DATA_AUDIT_PASSED, OrderStatusEnum.DATA_AUDIT_REJECTED);

            case DESIGN -> Set.of(OrderStatusEnum.DESIGNING, OrderStatusEnum.DESIGN_COMPLETED,
                    OrderStatusEnum.DESIGN_REVIEWING, OrderStatusEnum.DESIGN_REVIEW_REJECTED);

            case PRINT -> orderType == OrderTypeEnum.SERVICE
                    ? Set.of()
                    : Set.of(OrderStatusEnum.PENDING_PRINT, OrderStatusEnum.PRINTING, OrderStatusEnum.PRINT_COMPLETED);

            case POST_PROCESSING -> orderType == OrderTypeEnum.SERVICE
                    ? Set.of()
                    : Set.of(OrderStatusEnum.POST_PROCESSING);

            case QC -> orderType == OrderTypeEnum.SERVICE
                    ? Set.of()
                    : Set.of(OrderStatusEnum.QC_IN_PROGRESS, OrderStatusEnum.QC_FAILED, OrderStatusEnum.REWORK);

            case WAREHOUSE -> orderType == OrderTypeEnum.SERVICE
                    ? Set.of()
                    : Set.of(OrderStatusEnum.WAREHOUSE_IN, OrderStatusEnum.WAREHOUSED);

            case CONFIRM -> orderType == OrderTypeEnum.SERVICE
                    ? Set.of(OrderStatusEnum.AWAITING_CONFIRM)
                    : Set.of();

            case COMPLETED -> Set.of(OrderStatusEnum.COMPLETED);
        };
    }

    /**
     * 获取状态转换的说明
     *
     * @param fromStatus 起始状态
     * @param action 执行的动作
     * @return 转换说明
     */
    public String getTransitionDescription(Integer fromStatus, OrderActionEnum action) {
        OrderStatusEnum from = OrderStatusEnum.getByValue(fromStatus);
        Integer targetStatus = getTargetStatus(fromStatus, action);
        OrderStatusEnum to = targetStatus != null ? OrderStatusEnum.getByValue(targetStatus) : null;
        if (from == null || to == null) {
            return "状态转换失败";
        }
        return String.format("%s → %s", from.getName(), to.getName());
    }

    /**
     * 创建状态键（用于Map的key）
     */
    private static StatusKey statusKey(OrderPhaseEnum phase, OrderStatusEnum status) {
        return new StatusKey(phase, status);
    }

    /**
     * 状态键内部类
     * 用于作为Map的key
     */
    private record StatusKey(OrderPhaseEnum phase, OrderStatusEnum status) {}
}
