package com.yigongbao.flow.service;

import com.yigongbao.common.entity.FlowStatusHistoryEntity;
import com.yigongbao.flow.operator.FlowOperator;

import java.util.List;

/**
 * 流程状态历史 Service
 * 记录订单在各阶段的状态变更历史
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface FlowStatusHistoryService {

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
     * @param operator 操作人信息
     */
    void recordTransition(Long orderId, String orderCode, Integer phase,
                          Integer fromStatus, Integer toStatus,
                          String action, String actionName,
                          FlowOperator operator);

    /**
     * 查询订单的状态变更历史
     *
     * @param orderId 订单ID
     * @return 按时间顺序排列的历史记录
     */
    List<FlowStatusHistoryEntity> listByOrderId(Long orderId);

    /**
     * 查询订单的所有动作列表（按时间顺序）
     * 用于构建状态机上下文
     *
     * @param orderId 订单ID
     * @return 动作编码列表
     */
    List<String> listActionCodesByOrderId(Long orderId);
}
