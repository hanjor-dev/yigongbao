package com.yigongbao.module.order.service;

import com.yigongbao.module.order.entity.OrderStatusHistoryEntity;

/**
 * 订单状态历史 Service
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface OrderStatusHistoryService {

    /**
     * 记录状态变更
     *
     * @param orderId 订单ID
     * @param orderCode 订单编号
     * @param phase 阶段
     * @param status 状态
     * @param action 动作
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     */
    void recordTransition(Long orderId, String orderCode, Integer phase, Integer status,
                          String action, Long operatorId, String operatorName, String remark);
}
