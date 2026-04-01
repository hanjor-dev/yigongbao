package com.yigongbao.flow.facade;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;

import java.util.List;

/**
 * 流程 Facade
 * flow 模块的唯一对外入口，封装状态机和历史记录的完整操作
 *
 * 【设计原则】
 * - 对外只暴露 Facade，避免外部直接依赖内部 Service
 * - 内部组合 FlowStateMachineService 和 FlowStatusHistoryService
 * - 后续新增功能（如通知、审计等）可在 Facade 层统一编排
 *
 * @author hanjor
 * @date 2026-04-01
 */
public interface FlowFacade {

    /**
     * 查询当前可执行的动作列表
     *
     * @param orderId 业务实体ID（订单ID）
     * @return 可执行的动作编码列表
     */
    List<String> getAvailableActions(Long orderId);

    /**
     * 执行流程动作
     * 封装状态转换 + 历史记录的完整流程
     *
     * @param orderId 业务实体ID（订单ID）
     * @param action 动作枚举
     * @param operator 操作人信息
     * @return 转换结果（包含 phase 和 status）
     */
    TransitionResult executeFlow(Long orderId, FlowActionEnum action, FlowOperator operator);

}
