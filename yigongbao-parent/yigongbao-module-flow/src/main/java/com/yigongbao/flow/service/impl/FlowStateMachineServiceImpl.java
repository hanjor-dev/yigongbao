package com.yigongbao.flow.service.impl;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.flow.context.FlowContext;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.flow.rules.FlowPhaseTransitionRules;
import com.yigongbao.flow.rules.FlowPhaseTransitionRules.PhaseAndStatus;
import com.yigongbao.flow.rules.FlowStatusTransitionRules;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.flow.service.FlowStateMachineService;
import com.yigongbao.flow.service.FlowStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程状态机 Service 实现
 * 处理订单状态流转的核心业务逻辑，供订单及后续设计/生产模块复用
 *
 * 【核心逻辑】
 * - 订单流程分支由 needsPhysicalDelivery 决定
 * - needsPhysicalDelivery = 1（需要实体交付）：走完整生产流程
 * - needsPhysicalDelivery = 0（不需要实体交付）：跳过生产阶段
 * - DESIGN_REVIEW_PASSED 等不可见状态不落库，由状态机内部吸收并推进阶段
 *
 * 【阶段推进语义】
 * - 阶段推进时，最终落库的状态是"初始可见状态"而非"动作触发的目标状态"
 * - 例如 DATA_AUDIT_PASS 后，targetStatus=DATA_AUDIT_PASSED，但 DB 落库的是 DESIGNING
 * - 历史记录使用动作触发的目标状态，便于追溯
 *
 * 【职责边界】
 * - 此实现不直接更新数据库，仅返回 TransitionResult
 * - 调用方（order 模块的 OrderMainServiceImpl）负责根据 TransitionResult 更新订单
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlowStateMachineServiceImpl implements FlowStateMachineService {

    private final FlowOrderService flowOrderService;
    private final FlowStatusTransitionRules statusTransitionRules;
    private final FlowStatusHistoryService flowStatusHistoryService;

    /**
     * 查询当前可执行的动作
     *
     * 【实现逻辑】
     * 根据订单当前状态和阶段，通过 FlowStatusTransitionRules 获取允许的动作列表
     *
     * @param orderId 订单ID
     * @return 可执行的动作列表（动作编码列表）
     */
    @Override
    public List<String> getAvailableActions(Long orderId) {
        OrderMainEntity order = flowOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        log.debug("查询订单可执行动作，orderId={}, status={}, phase={}",
                order.getId(), order.getStatus(), order.getPhase());
        List<FlowActionEnum> availableActions = statusTransitionRules.getAvailableActions(
                order.getStatus(), order.getPhase(), order.getNeedsPhysicalDelivery());
        return availableActions.stream()
                .map(FlowActionEnum::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 执行状态转换，同时处理阶段推进
     *
     * 【核心流程】
     * 1. 校验订单存在
     * 2. CREATE 动作：仅记录历史，不改变状态
     * 3. 校验循环次数：防止无限驳回/返工/设计审核驳回
     * 4. 校验动作可执行性：当前状态是否允许执行此动作
     * 5. 获取目标状态：动作触发的目标状态
     * 6. 决策阶段推进：根据目标状态判断是否需要推进到下一阶段
     * 7. 不可见状态处理：DESIGN_REVIEW_PASSED 等不可见状态不落库
     * 8. 记录状态历史：落库变更记录
     * 9. 返回转换结果：供调用方更新数据库
     *
     * @param orderId 订单ID
     * @param action 动作枚举
     * @param operator 操作人信息
     * @return 阶段推进结果（包含 phase 和 status）
     */
    @Override
    public TransitionResult executeTransition(Long orderId, FlowActionEnum action, FlowOperator operator) {
        // Step 1: 校验订单存在
        OrderMainEntity order = flowOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        log.info("执行订单状态转换，orderId={}, currentPhase={}, currentStatus={}, action={}, operatorId={}",
                order.getId(), order.getPhase(), order.getStatus(), action.getCode(),
                operator != null ? operator.getOperatorId() : null);

        Integer fromStatus = order.getStatus();
        FlowPhaseEnum currentPhase = FlowPhaseEnum.getByValue(order.getPhase());
        // 防御：phase 为 null 时视为非法数据，拒绝流转
        if (currentPhase == null) {
            log.error("订单阶段数据异常，orderId={}, phase={}", order.getId(), order.getPhase());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR);
        }

        try {
            // ========== Step 2: CREATE 动作：仅记录历史，不改变状态 ==========
            if (action == FlowActionEnum.CREATE) {
                flowStatusHistoryService.recordTransition(
                        order.getId(), order.getOrderCode(), order.getPhase(),
                        fromStatus, fromStatus,
                        action.getCode(), action.getName(),
                        operator);
                log.info("CREATE 动作仅记录历史，orderId={}, status={}", order.getId(), fromStatus);
                return TransitionResult.of(currentPhase.getValue(), fromStatus);
            }

            // ========== Step 3: 校验循环次数 ==========
            // 防止无限驳回/返工/设计审核驳回
            FlowContext ctx = buildContextFromHistory(order.getId());
            applyContextAction(ctx, action);
            ctx.validateNoExcessiveLoops();

            // ========== Step 4: 校验动作是否可执行 ==========
            if (!statusTransitionRules.canExecuteAction(fromStatus, order.getPhase(),
                    order.getNeedsPhysicalDelivery(), action)) {
                log.warn("当前状态下不允许执行此动作，orderId={}, currentStatus={}, action={}",
                        order.getId(), fromStatus, action.getCode());
                throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR);
            }

            // ========== Step 5: 获取动作触发的目标状态 ==========
            Integer targetStatusValue = statusTransitionRules.getTargetStatus(fromStatus, action);
            if (targetStatusValue == null) {
                log.warn("状态转换不合法，currentStatus={}, action={}", fromStatus, action.getCode());
                throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR);
            }
            FlowStatusEnum targetStatus = FlowStatusEnum.getByValue(targetStatusValue);

            // ========== Step 6: 决策下一阶段及初始状态 ==========
            PhaseAndStatus phaseAndStatus = FlowPhaseTransitionRules.decideNextPhaseAndStatus(
                    currentPhase, targetStatus, action, order.getNeedsPhysicalDelivery());
            Integer nextPhase = phaseAndStatus != null && phaseAndStatus.phase() != null
                    ? phaseAndStatus.phase().getValue() : null;
            Integer initialStatus = phaseAndStatus != null && phaseAndStatus.initialStatus() != null
                    ? phaseAndStatus.initialStatus().getValue() : null;

            // ========== Step 7: 不可见状态处理 ==========
            // 不可见状态：历史记录不可见状态，落库初始可见状态
            if (FlowPhaseTransitionRules.isInvisibleStatus(targetStatus)) {
                log.info("检测到不可见状态{}，自动推进阶段，nextPhase={}, initialStatus={}",
                        targetStatus, nextPhase, initialStatus);

                flowStatusHistoryService.recordTransition(
                        order.getId(), order.getOrderCode(), currentPhase.getValue(),
                        fromStatus, targetStatusValue,
                        action.getCode(), action.getName(),
                        operator);

                log.info("不可见状态推进完成，orderId={}, nextPhase={}, initialStatus={}",
                        order.getId(), nextPhase, initialStatus);
                return TransitionResult.ofWithPhaseChange(nextPhase, targetStatusValue, initialStatus);
            }

            // ========== Step 8: 正常状态 - 记录历史 ==========
            // phase 取推进后的阶段（如果有），否则取当前阶段
            Integer recordPhase = (nextPhase != null) ? nextPhase : currentPhase.getValue();
            flowStatusHistoryService.recordTransition(
                    order.getId(), order.getOrderCode(), recordPhase,
                    fromStatus, targetStatusValue,
                    action.getCode(), action.getName(),
                    operator);

            // ========== Step 9: 构建返回结果 ==========
            if (nextPhase != null) {
                // 阶段推进：落库 initialStatus
                Integer finalStatus = initialStatus != null ? initialStatus : targetStatusValue;
                log.info("阶段推进，orderId={}, fromPhase={}, toPhase={}, finalStatus={}",
                        order.getId(), currentPhase.getValue(), nextPhase, finalStatus);
                return TransitionResult.ofWithPhaseChange(nextPhase, targetStatusValue, finalStatus);
            } else {
                // 状态不变
                log.info("执行订单状态转换成功，orderId={}, fromStatus={}, toStatus={}",
                        order.getId(), fromStatus, targetStatusValue);
                return TransitionResult.of(currentPhase.getValue(), targetStatusValue);
            }

        } catch (Exception e) {
            log.error("执行订单状态转换异常，orderId={}, action={}", order.getId(), action.getCode(), e);
            throw e;
        }
    }

    /**
     * 从历史记录中构建状态机上下文
     *
     * 【使用场景】
     * 执行动作前，需要统计历史中的驳回/返工次数，用于判断是否超过上限
     *
     * @param orderId 订单ID
     * @return 状态机上下文
     */
    private FlowContext buildContextFromHistory(Long orderId) {
        List<String> actionCodes = flowStatusHistoryService.listActionCodesByOrderId(orderId);
        return FlowContext.buildFromHistory(actionCodes);
    }

    /**
     * 根据当前动作更新上下文计数
     * 用于统计各类循环次数（审核驳回、返工、设计审核驳回）
     *
     * @param ctx 状态机上下文
     * @param action 触发动作
     */
    private void applyContextAction(FlowContext ctx, FlowActionEnum action) {
        switch (action) {
            case DATA_AUDIT_REJECT -> ctx.incrementAuditReject();
            case REWORK -> ctx.incrementRework();
            case DESIGN_REVIEW_REJECT -> ctx.incrementDesignReject();
            default -> { }
        }
    }
}
