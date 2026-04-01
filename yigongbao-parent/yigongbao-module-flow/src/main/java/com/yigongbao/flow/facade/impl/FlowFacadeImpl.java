package com.yigongbao.flow.facade.impl;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.flow.service.FlowStateMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程 Facade 实现
 * 作为 flow 模块的唯一对外入口，封装状态机和历史记录的完整操作
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowFacadeImpl implements FlowFacade {

    private final FlowStateMachineService flowStateMachineService;

    /**
     * 查询当前可执行的动作列表
     * 直接委托 FlowStateMachineService 执行
     *
     * @param orderId 订单ID
     * @return 可执行的动作编码列表
     */
    @Override
    public List<String> getAvailableActions(Long orderId) {
        return flowStateMachineService.getAvailableActions(orderId);
    }

    /**
     * 执行流程动作
     * 封装状态转换 + 历史记录的完整流程
     *
     * 【防御性处理】
     * - operator 为 null 时使用空对象，避免 NPE
     *
     * @param orderId 订单ID
     * @param action 动作枚举
     * @param operator 操作人信息（允许为 null）
     * @return 转换结果（包含 phase 和 status）
     */
    @Override
    public TransitionResult executeFlow(Long orderId, FlowActionEnum action, FlowOperator operator) {
        // 防御：operator 为 null 时使用空对象
        if (operator == null) {
            operator = new FlowOperator();
        }
        log.info("FlowFacade 执行流程动作，orderId={}, action={}, operatorId={}",
                orderId, action.getCode(), operator.getOperatorId());
        TransitionResult result = flowStateMachineService.executeTransition(orderId, action, operator);
        log.info("FlowFacade 执行完成，orderId={}, result={}", orderId, result);
        return result;
    }

}
