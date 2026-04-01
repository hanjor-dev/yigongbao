package com.yigongbao.flow.service;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;

import java.util.List;

/**
 * 流程状态机 Service
 * 处理订单状态流转的核心业务逻辑，供订单及后续设计/生产模块复用
 *
 * 【核心职责】
 * - 查询当前可执行的动作列表
 * - 执行状态转换（同时处理阶段推进和不可见状态）
 * - 记录状态变更历史
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface FlowStateMachineService {

    /**
     * 查询当前可执行的动作
     *
     * @param orderId 订单ID
     * @return 可执行的动作列表（动作编码列表）
     */
    List<String> getAvailableActions(Long orderId);

    /**
     * 执行状态转换，同时处理阶段推进
     *
     * 【核心逻辑】
     * 1. 根据当前状态和动作获取目标状态
     * 2. 判断目标状态是否为不可见状态（DESIGN_REVIEW_PASSED）
     *    - 如果是：自动推进到下一阶段，不落库中间状态
     *    - 如果否：落库，同时决策是否需要推进阶段
     * 3. 记录状态历史
     *
     * @param orderId 订单ID
     * @param action 动作枚举
     * @param operator 操作人信息
     * @return 阶段推进结果（包含 phase 和 status）
     */
    TransitionResult executeTransition(Long orderId, FlowActionEnum action, FlowOperator operator);
}
