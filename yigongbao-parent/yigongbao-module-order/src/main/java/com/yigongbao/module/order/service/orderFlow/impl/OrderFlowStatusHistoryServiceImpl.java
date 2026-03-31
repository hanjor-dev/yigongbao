package com.yigongbao.module.order.service.orderFlow.impl;

import com.yigongbao.module.order.entity.orderFlow.OrderFlowStatusHistoryEntity;
import com.yigongbao.module.order.mapper.orderFlow.OrderFlowStatusHistoryMapper;
import com.yigongbao.module.order.service.orderFlow.OrderFlowStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单流程状态历史 Service 实现
 * 处理订单状态变更历史的记录
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderFlowStatusHistoryServiceImpl implements OrderFlowStatusHistoryService {

    private final OrderFlowStatusHistoryMapper orderFlowStatusHistoryMapper;

    /**
     * 记录状态变更
     *
     * @param orderId 订单ID
     * @param orderCode 订单编号
     * @param phase 变更时阶段
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param action 触发动作
     * @param actionName 动作名称
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     */
    @Override
    public void recordTransition(Long orderId, String orderCode, Integer phase,
                                Integer fromStatus, Integer toStatus,
                                String action, String actionName,
                                Long operatorId, String operatorName, String remark) {
        log.info("记录订单状态变更，orderId={}, orderCode={}, phase={}, fromStatus={}, toStatus={}, action={}, operatorId={}",
                orderId, orderCode, phase, fromStatus, toStatus, action, operatorId);
        try {
            OrderFlowStatusHistoryEntity entity = new OrderFlowStatusHistoryEntity();
            entity.setOrderId(orderId);
            entity.setOrderCode(orderCode);
            entity.setPhase(phase);
            entity.setFromStatus(fromStatus);
            entity.setToStatus(toStatus);
            entity.setAction(action);
            entity.setActionName(actionName);
            entity.setOperatorId(operatorId);
            entity.setOperatorName(operatorName);
            entity.setRemark(remark);
            orderFlowStatusHistoryMapper.insert(entity);
            log.info("记录订单状态变更成功，id={}", entity.getId());
        } catch (Exception e) {
            log.error("记录订单状态变更异常，orderId={}, orderCode={}", orderId, orderCode, e);
            throw e;
        }
    }
}
