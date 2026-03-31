package com.yigongbao.module.order.service.orderFlow;

import com.yigongbao.module.order.entity.orderFlow.OrderFlowStatusHistoryEntity;

/**
 * 订单流程状态历史 Service
 * 记录订单在各阶段的状态变更历史
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface OrderFlowStatusHistoryService {

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
    void recordTransition(Long orderId, String orderCode, Integer phase,
                          Integer fromStatus, Integer toStatus,
                          String action, String actionName,
                          Long operatorId, String operatorName, String remark);
}
