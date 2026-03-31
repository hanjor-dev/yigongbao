package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.order.OrderActionEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.rules.OrderStatusTransitionRules;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.service.OrderStateMachineService;
import com.yigongbao.module.order.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单状态机 Service 实现
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStateMachineServiceImpl implements OrderStateMachineService {

    private final OrderStatusTransitionRules statusTransitionRules;
    private final OrderStatusHistoryService statusHistoryService;

    @Override
    public List<String> getAvailableActions(OrderMainEntity order) {
        log.info("查询订单可执行动作，orderId={}, status={}", order.getId(), order.getStatus());
        List<OrderActionEnum> availableActions = statusTransitionRules.getAvailableActions(order.getStatus());
        List<String> actionCodes = availableActions.stream().map(OrderActionEnum::getCode).collect(Collectors.toList());
        log.info("查询订单可执行动作成功，orderId={}, actions={}", order.getId(), actionCodes);
        return actionCodes;
    }

    @Override
    public Integer executeTransition(OrderMainEntity order, String action, Long operatorId, String operatorName, String remark) {
        log.info("执行订单状态转换，orderId={}, currentStatus={}, action={}, operatorId={}",
                order.getId(), order.getStatus(), action, operatorId);
        OrderActionEnum actionEnum = OrderActionEnum.getByCode(action);
        if (actionEnum == null) {
            log.warn("动作不合法，action={}", action);
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        Integer targetStatus = statusTransitionRules.getTargetStatus(order.getStatus(), actionEnum);
        if (targetStatus == null) {
            log.warn("状态转换不合法，currentStatus={}, action={}", order.getStatus(), action);
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR);
        }
        statusHistoryService.recordTransition(order.getId(), order.getOrderCode(), order.getPhase(), targetStatus,
                action, operatorId, operatorName, remark);
        log.info("执行订单状态转换成功，orderId={}, targetStatus={}", order.getId(), targetStatus);
        return targetStatus;
    }
}
