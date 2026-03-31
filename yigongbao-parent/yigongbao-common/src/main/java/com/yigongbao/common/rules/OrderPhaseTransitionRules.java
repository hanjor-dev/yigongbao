package com.yigongbao.common.rules;

import com.yigongbao.common.enums.order.OrderPhaseEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
    private static final List<OrderPhaseEnum> ORDER_NEXT = Arrays.asList(
            OrderPhaseEnum.DESIGN
    );

    private static final List<OrderPhaseEnum> DESIGN_NEXT = Arrays.asList(
            OrderPhaseEnum.PRODUCTION
    );

    private static final List<OrderPhaseEnum> PRODUCTION_NEXT = Arrays.asList();

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
        return switch (phase) {
            case ORDER -> ORDER_NEXT.stream().map(OrderPhaseEnum::getValue).toList();
            case DESIGN -> DESIGN_NEXT.stream().map(OrderPhaseEnum::getValue).toList();
            case PRODUCTION -> PRODUCTION_NEXT.stream().map(OrderPhaseEnum::getValue).toList();
        };
    }
}
