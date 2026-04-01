package com.yigongbao.flow.service;

import com.yigongbao.common.entity.OrderMainEntity;

/**
 * 流程订单基础 Service
 * 提供订单主表的基础查询能力，供状态机、状态历史等通用流程模块使用
 *
 * 【设计说明】
 * 此接口是 flow 模块与 order 模块的"解耦桥梁"
 * - flow 模块通过此接口获取订单数据，不直接依赖 order 模块
 * - order 模块通过 Spring 依赖注入实现此接口，提供实际查询能力
 * - 调用方（FlowStateMachineService 等）仅依赖接口，不感知具体实现
 *
 * 【职责范围】
 * - 仅包含"查询"和"通用状态更新"操作
 * - 不包含业务逻辑（如校验、审批等），业务逻辑由调用方自行处理
 *
 * @author hanjor
 * @date 2026-04-01
 */
public interface FlowOrderService {

    /**
     * 根据订单ID查询订单
     *
     * @param id 订单ID
     * @return 订单实体，如果不存在返回 null
     */
    OrderMainEntity getById(Long id);

    /**
     * 更新订单阶段和状态
     * 供状态机执行完状态转换后调用，落库新的阶段和状态
     *
     * @param id 订单ID
     * @param phase 目标阶段
     * @param status 目标状态
     */
    void updatePhaseAndStatus(Long id, Integer phase, Integer status);

    /**
     * 更新订单阶段、状态和当前处理人
     *
     * @param id 订单ID
     * @param phase 目标阶段
     * @param status 目标状态
     * @param currentHandlerId 当前处理人ID
     */
    void updatePhaseAndStatusWithHandler(Long id, Integer phase, Integer status, Long currentHandlerId);
}
