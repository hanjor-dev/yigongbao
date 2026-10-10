package com.yigongbao.flow.facade.impl;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.flow.service.FlowStateMachineService;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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
    private final FlowOrderService flowOrderService;
    private final FlowStatusHistoryService flowStatusHistoryService;

    private static final Set<FlowActionEnum> AUDIT_ACTIONS = Set.of(
            FlowActionEnum.DATA_AUDIT_PASS,
            FlowActionEnum.DATA_AUDIT_REJECT
    );

    @Override
    public List<String> getAvailableActions(Long orderId) {
        return flowStateMachineService.getAvailableActions(orderId);
    }

    @Override
    public TransitionResult executeFlow(Long orderId, FlowActionEnum action, FlowOperator operator) {
        if (operator == null) {
            operator = new FlowOperator();
        }
        log.info("FlowFacade 执行流程动作，orderId={}, action={}, operatorId={}",
                orderId, action.getCode(), operator.getOperatorId());
        TransitionResult result = flowStateMachineService.executeTransition(orderId, action, operator);
        log.info("FlowFacade 执行完成，orderId={}, result={}", orderId, result);
        return result;
    }

    @Override
    public TransitionResult executeFlow(Long orderId, FlowActionEnum action,
            FlowOperator operator, Integer expectedVersion) {
        if (operator == null) {
            operator = new FlowOperator();
        }
        if (AUDIT_ACTIONS.contains(action)) {
            // 将 null 视为 0（新订单的初始版本）
            Integer expected = expectedVersion != null ? expectedVersion : 0;
            OrderMainEntity order = flowOrderService.getById(orderId);
            if (order == null) {
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            Integer actual = order.getVersion() != null ? order.getVersion() : 0;
            if (!expected.equals(actual)) {
                log.warn("订单版本冲突，orderId={}, expectedVersion={}, actualVersion={}",
                        orderId, expected, actual);
                throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
            }
        }
        log.info("FlowFacade 执行流程动作（带版本校验），orderId={}, action={}, expectedVersion={}",
                orderId, action.getCode(), expectedVersion);
        return flowStateMachineService.executeTransition(orderId, action, operator);
    }

    @Override
    public TransitionResult executeAdditionalDesignStart(Long orderId, FlowOperator operator,
                                                         Integer expectedVersion) {
        OrderMainEntity order = flowOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (operator == null) {
            operator = new FlowOperator();
        }
        Integer fromStatus = order.getStatus();
        if (FlowStatusEnum.DESIGN_IN_PROGRESS.getValue().equals(fromStatus)) {
            return TransitionResult.of(FlowPhaseEnum.DESIGN.getValue(), fromStatus);
        }
        Set<Integer> allowedSourceStatuses = Set.of(
                FlowStatusEnum.DESIGN_COMPLETED.getValue(),
                FlowStatusEnum.PENDING_PRINT.getValue(),
                FlowStatusEnum.PRINTING.getValue(),
                FlowStatusEnum.PRINT_COMPLETED.getValue(),
                FlowStatusEnum.POST_PROCESSING.getValue(),
                FlowStatusEnum.QC_IN_PROGRESS.getValue(),
                FlowStatusEnum.QC_FAILED.getValue(),
                FlowStatusEnum.REWORK.getValue(),
                FlowStatusEnum.QC_PASSED.getValue(),
                FlowStatusEnum.PACKING.getValue(),
                FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue(),
                FlowStatusEnum.WAREHOUSED.getValue(),
                FlowStatusEnum.WAREHOUSE_OUT.getValue(),
                FlowStatusEnum.COMPLETED.getValue());
        if (!allowedSourceStatuses.contains(fromStatus)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        Integer expected = expectedVersion != null ? expectedVersion
                : (order.getVersion() != null ? order.getVersion() : 0);
        FlowActionEnum action = FlowActionEnum.START_ADDITIONAL_DESIGN;

        flowStatusHistoryService.recordTransition(
                order.getId(), order.getOrderCode(), FlowPhaseEnum.DESIGN.getValue(),
                fromStatus, FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(),
                action.getCode(), action.getName(), operator);

        int updated = flowOrderService.updatePhaseAndStatusWithHandlerIfVersion(
                orderId, expected, FlowPhaseEnum.DESIGN.getValue(),
                FlowStatusEnum.DESIGN_IN_PROGRESS.getValue(),
                operator.getOperatorId(), operator.getOperatorName());
        if (updated != 1) {
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }
        return TransitionResult.of(FlowPhaseEnum.DESIGN.getValue(),
                FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
    }
}
