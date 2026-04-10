package com.yigongbao.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.FlowStatusHistoryEntity;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.mapper.FlowStatusHistoryMapper;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程状态历史 Service 实现
 * 处理订单状态变更历史的记录和查询
 *
 * 【职责说明】
 * - 记录每一次状态变更，包括变更前后状态、触发动作、操作人等信息
 * - 查询订单的状态变更历史，供前端展示和 FlowContext 重建
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowStatusHistoryServiceImpl implements FlowStatusHistoryService {

    private final FlowStatusHistoryMapper flowStatusHistoryMapper;

    /**
     * 记录状态变更
     *
     * 【实现说明】
     * - 记录从 fromStatus 到 toStatus 的状态变化
     * - phase 字段记录变更发生时的阶段（推进后使用下一阶段的 phase）
     * - 由状态机在执行完状态转换后调用
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
    @Override
    public void recordTransition(Long orderId, String orderCode, Integer phase,
                                Integer fromStatus, Integer toStatus,
                                String action, String actionName,
                                FlowOperator operator) {
        log.info("记录订单状态变更，orderId={}, orderCode={}, phase={}, fromStatus={}, toStatus={}, action={}, operatorId={}",
                orderId, orderCode, phase, fromStatus, toStatus, action, operator.getOperatorId());
        try {
            FlowStatusHistoryEntity entity = new FlowStatusHistoryEntity();
            entity.setOrderId(orderId);
            entity.setOrderCode(orderCode);
            entity.setPhase(phase);
            // 快照阶段和状态名称，防止枚举后续改名导致历史展示错误
            FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
            entity.setPhaseName(phaseEnum != null ? phaseEnum.getName() : null);
            entity.setFromStatus(fromStatus);
            FlowStatusEnum fromStatusEnum = FlowStatusEnum.getByValue(fromStatus);
            entity.setFromStatusName(fromStatusEnum != null ? fromStatusEnum.getName() : null);
            entity.setToStatus(toStatus);
            FlowStatusEnum toStatusEnum = FlowStatusEnum.getByValue(toStatus);
            entity.setToStatusName(toStatusEnum != null ? toStatusEnum.getName() : null);
            entity.setAction(action);
            entity.setActionName(actionName);
            entity.setOperatorId(operator.getOperatorId());
            entity.setOperatorName(operator.getOperatorName());
            entity.setRemark(operator.getRemark());
            flowStatusHistoryMapper.insert(entity);
            log.info("记录订单状态变更成功，id={}", entity.getId());
        } catch (Exception e) {
            log.error("记录订单状态变更异常，orderId={}, orderCode={}", orderId, orderCode, e);
            throw e;
        }
    }

    /**
     * 查询订单的状态变更历史
     *
     * @param orderId 订单ID
     * @return 按时间顺序排列的历史记录
     */
    @Override
    public List<FlowStatusHistoryEntity> listByOrderId(Long orderId) {
        LambdaQueryWrapper<FlowStatusHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowStatusHistoryEntity::getOrderId, orderId)
                .orderByAsc(FlowStatusHistoryEntity::getCreateTime);
        return flowStatusHistoryMapper.selectList(wrapper);
    }

    /**
     * 查询订单的所有动作列表（按时间顺序）
     * 用于构建状态机上下文 FlowContext
     *
     * 【使用场景】
     * - FlowStateMachineService 执行动作前，通过此方法重建 FlowContext
     * - FlowContext 统计审核驳回、返工、设计审核驳回等循环次数
     *
     * @param orderId 订单ID
     * @return 动作编码列表
     */
    @Override
    public List<String> listActionCodesByOrderId(Long orderId) {
        return listByOrderId(orderId).stream()
                .map(FlowStatusHistoryEntity::getAction)
                .collect(Collectors.toList());
    }
}
