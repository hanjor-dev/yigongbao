package com.yigongbao.module.order.service;

import com.yigongbao.module.order.entity.OrderMainEntity;

import java.util.List;

/**
 * 订单状态机 Service
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface OrderStateMachineService {

    /**
     * 查询当前可执行的动作
     *
     * @param order 订单实体
     * @return 可执行的动作列表
     */
    List<String> getAvailableActions(OrderMainEntity order);

    /**
     * 执行状态转换
     *
     * @param order 订单实体
     * @param action 动作
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param remark 备注
     * @return 目标状态
     */
    Integer executeTransition(OrderMainEntity order, String action, Long operatorId, String operatorName, String remark);
}
