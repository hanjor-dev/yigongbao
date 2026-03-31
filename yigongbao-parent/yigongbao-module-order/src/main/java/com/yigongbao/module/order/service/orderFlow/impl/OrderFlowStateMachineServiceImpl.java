package com.yigongbao.module.order.service.orderFlow.impl;

import com.yigongbao.common.enums.order.OrderActionEnum;
import com.yigongbao.common.enums.order.OrderStatusEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.rules.OrderStatusTransitionRules;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.service.orderFlow.OrderFlowStateMachineService;
import com.yigongbao.module.order.service.orderFlow.OrderFlowStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单流程状态机 Service 实现
 * 处理订单状态流转的核心业务逻辑
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFlowStateMachineServiceImpl implements OrderFlowStateMachineService {

    private final OrderStatusTransitionRules statusTransitionRules;
    private final OrderFlowStatusHistoryService orderFlowStatusHistoryService;

    /**
     * 查询当前可执行的动作
     *
     * @param order 订单实体
     * @return 可执行的动作列表（动作编码列表）
     */
    @Override
    public List<String> getAvailableActions(OrderMainEntity order) {
        log.info("查询订单可执行动作，orderId={}, status={}", order.getId(), order.getStatus());
        try {
            // 从状态转换规则中获取当前状态可执行的动作（传入 status、phase、orderType）
            List<OrderActionEnum> availableActions = statusTransitionRules.getAvailableActions(
                    order.getStatus(), order.getPhase(), order.getOrderType());
            // 提取动作编码列表
            List<String> actionCodes = availableActions.stream()
                    .map(OrderActionEnum::getCode)
                    .collect(Collectors.toList());
            log.info("查询订单可执行动作成功，orderId={}, actions={}", order.getId(), actionCodes);
            return actionCodes;
        } catch (Exception e) {
            log.error("查询订单可执行动作异常，orderId={}", order.getId(), e);
            throw e;
        }
    }

    /**
     * 执行状态转换
     *
     * @param order 订单实体
     * @param action 动作枚举（非动作编码字符串）
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     * @return 目标状态
     */
    @Override
    public Integer executeTransition(OrderMainEntity order, OrderActionEnum action,
                                     Long operatorId, String operatorName, String remark) {
        log.info("执行订单状态转换，orderId={}, currentStatus={}, action={}, operatorId={}",
                order.getId(), order.getStatus(), action.getCode(), operatorId);
        try {
            // 记录变更前状态
            Integer fromStatus = order.getStatus();
            // 根据当前状态和动作获取目标状态
            Integer targetStatus = statusTransitionRules.getTargetStatus(fromStatus, action);
            if (targetStatus == null) {
                log.warn("状态转换不合法，currentStatus={}, action={}", fromStatus, action.getCode());
                throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR);
            }
            // 记录状态变更历史
            orderFlowStatusHistoryService.recordTransition(
                    order.getId(), order.getOrderCode(), order.getPhase(),
                    fromStatus, targetStatus,
                    action.getCode(), action.getName(),
                    operatorId, operatorName, remark);
            log.info("执行订单状态转换成功，orderId={}, fromStatus={}, toStatus={}",
                    order.getId(), fromStatus, targetStatus);
            return targetStatus;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行订单状态转换异常，orderId={}, action={}", order.getId(), action.getCode(), e);
            throw e;
        }
    }
}
