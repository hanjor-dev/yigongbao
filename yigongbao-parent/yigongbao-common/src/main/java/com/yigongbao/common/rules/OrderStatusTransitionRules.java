package com.yigongbao.common.rules;

import com.yigongbao.common.enums.order.OrderActionEnum;
import com.yigongbao.common.enums.order.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 订单状态转换规则
 * 定义订单阶段内部的状态转换规则
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Slf4j
@Component
public class OrderStatusTransitionRules {

    /**
     * 获取当前状态可执行的动作
     *
     * @param currentStatus 当前状态值
     * @return 可执行的动作列表
     */
    public List<OrderActionEnum> getAvailableActions(Integer currentStatus) {
        if (currentStatus == null) {
            return List.of();
        }
        OrderStatusEnum status = OrderStatusEnum.getByValue(currentStatus);
        if (status == null) {
            return List.of();
        }
        return switch (status) {
            case DRAFT -> List.of(OrderActionEnum.SUBMIT);
            case PENDING -> List.of(OrderActionEnum.WITHDRAW, OrderActionEnum.AUDIT_PASS, OrderActionEnum.AUDIT_REJECT);
            case PROCESSING -> List.of(OrderActionEnum.COMPLETE, OrderActionEnum.CANCEL, OrderActionEnum.PHASE_TRANSFER);
            case COMPLETED -> List.of();
            case CANCELLED -> List.of();
        };
    }

    /**
     * 判断当前状态是否可以执行指定动作
     *
     * @param currentStatus 当前状态值
     * @param action 要执行的动作
     * @return true-可以，false-不可以
     */
    public boolean canExecuteAction(Integer currentStatus, OrderActionEnum action) {
        if (currentStatus == null || action == null) {
            return false;
        }
        return getAvailableActions(currentStatus).contains(action);
    }

    /**
     * 获取动作执行后的目标状态
     *
     * @param currentStatus 当前状态值
     * @param action 要执行的动作
     * @return 目标状态值，如果无法执行则返回 null
     */
    public Integer getTargetStatus(Integer currentStatus, OrderActionEnum action) {
        if (!canExecuteAction(currentStatus, action)) {
            return null;
        }
        return switch (action) {
            case SUBMIT -> OrderStatusEnum.PENDING.getValue();
            case WITHDRAW -> OrderStatusEnum.DRAFT.getValue();
            case AUDIT_PASS -> OrderStatusEnum.PROCESSING.getValue();
            case AUDIT_REJECT -> OrderStatusEnum.PENDING.getValue();
            case RESUBMIT -> OrderStatusEnum.PENDING.getValue();
            case COMPLETE -> OrderStatusEnum.COMPLETED.getValue();
            case CANCEL -> OrderStatusEnum.CANCELLED.getValue();
            case PHASE_TRANSFER -> OrderStatusEnum.PROCESSING.getValue();
            default -> null;
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
}
