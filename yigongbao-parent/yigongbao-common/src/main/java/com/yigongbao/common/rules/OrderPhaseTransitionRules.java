package com.yigongbao.common.rules;

import com.yigongbao.common.enums.order.OrderPhaseEnum;
import com.yigongbao.common.enums.order.OrderTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 订单阶段流转规则
 * 定义各阶段之间的流转关系
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Slf4j
@Component
public class OrderPhaseTransitionRules implements PhaseTransitionRule {

    /**
     * 阶段流转规则映射
     * key: 当前阶段 value: 可流转的下一阶段列表
     */
    private static final Map<OrderPhaseEnum, Set<OrderPhaseEnum>> PHASE_TRANSITIONS = Map.of(
            OrderPhaseEnum.ORDER, Set.of(OrderPhaseEnum.DESIGN),
            OrderPhaseEnum.DESIGN, Set.of(OrderPhaseEnum.PRINT, OrderPhaseEnum.CONFIRM),
            OrderPhaseEnum.PRINT, Set.of(OrderPhaseEnum.POST_PROCESSING),
            OrderPhaseEnum.POST_PROCESSING, Set.of(OrderPhaseEnum.QC),
            OrderPhaseEnum.QC, Set.of(OrderPhaseEnum.WAREHOUSE),
            OrderPhaseEnum.WAREHOUSE, Set.of(OrderPhaseEnum.COMPLETED),
            OrderPhaseEnum.CONFIRM, Set.of(OrderPhaseEnum.COMPLETED),
            OrderPhaseEnum.COMPLETED, Set.of()
    );

    @Override
    public boolean canTransition(Integer fromPhase, Integer toPhase) {
        if (fromPhase == null || toPhase == null) {
            return false;
        }
        OrderPhaseEnum from = OrderPhaseEnum.getByValue(fromPhase);
        OrderPhaseEnum to = OrderPhaseEnum.getByValue(toPhase);
        if (from == null || to == null) {
            return false;
        }
        List<Integer> availableNextValues = getAvailableNextPhases(fromPhase);
        return availableNextValues.contains(to.getValue());
    }

    @Override
    public List<Integer> getAvailableNextPhases(Integer currentPhase) {
        if (currentPhase == null) {
            return List.of();
        }
        OrderPhaseEnum phase = OrderPhaseEnum.getByValue(currentPhase);
        if (phase == null) {
            return List.of();
        }
        Set<OrderPhaseEnum> allowed = PHASE_TRANSITIONS.getOrDefault(phase, Set.of());
        return allowed.stream().map(OrderPhaseEnum::getValue).toList();
    }

    /**
     * 判断阶段转换是否合法
     *
     * @param fromPhase 当前阶段
     * @param toPhase   目标阶段
     * @param orderType 订单类型（用于判断服务订单是否可进入打印/质检/仓储）
     * @return 是否合法
     */
    public static boolean isValidPhaseTransition(
            OrderPhaseEnum fromPhase,
            OrderPhaseEnum toPhase,
            OrderTypeEnum orderType) {

        // 已完成状态不可跳转
        if (fromPhase == OrderPhaseEnum.COMPLETED) {
            return false;
        }

        // 服务订单不能进入打印/后处理/质检/仓储阶段
        if (orderType == OrderTypeEnum.SERVICE) {
            if (toPhase == OrderPhaseEnum.PRINT || toPhase == OrderPhaseEnum.POST_PROCESSING
                    || toPhase == OrderPhaseEnum.QC || toPhase == OrderPhaseEnum.WAREHOUSE) {
                return false;
            }
        }

        // 校验阶段转换是否在允许范围内
        Set<OrderPhaseEnum> allowedPhases = PHASE_TRANSITIONS.getOrDefault(fromPhase, Set.of());
        return allowedPhases.contains(toPhase);
    }

    /**
     * 获取指定阶段的下一个默认阶段
     * 用于阶段推进时的自动跳转
     *
     * @param currentPhase 当前阶段
     * @param orderType    订单类型
     * @return 下一个阶段，如果没有则返回null
     */
    public static OrderPhaseEnum getNextPhase(OrderPhaseEnum currentPhase, OrderTypeEnum orderType) {
        return switch (currentPhase) {
            case ORDER -> OrderPhaseEnum.DESIGN;
            case DESIGN -> orderType == OrderTypeEnum.SERVICE ? OrderPhaseEnum.CONFIRM : OrderPhaseEnum.PRINT;
            case PRINT -> OrderPhaseEnum.POST_PROCESSING;
            case POST_PROCESSING -> OrderPhaseEnum.QC;
            case QC -> OrderPhaseEnum.WAREHOUSE;
            case WAREHOUSE -> OrderPhaseEnum.COMPLETED;
            case CONFIRM -> OrderPhaseEnum.COMPLETED;
            case COMPLETED -> null;
        };
    }
}
