package com.yigongbao.flow.rules;

import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 阶段流转规则
 * 定义各阶段之间的流转关系
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Slf4j
public class FlowPhaseTransitionRules implements FlowTransitionRule {

    /**
     * 阶段流转规则映射
     * key: 当前阶段 value: 可流转的下一阶段列表
     */
    private static final Map<FlowPhaseEnum, Set<FlowPhaseEnum>> PHASE_TRANSITIONS = Map.of(
            FlowPhaseEnum.ORDER, Set.of(FlowPhaseEnum.DESIGN),
            FlowPhaseEnum.DESIGN, Set.of(FlowPhaseEnum.PRINT, FlowPhaseEnum.CONFIRM),
            FlowPhaseEnum.PRINT, Set.of(FlowPhaseEnum.POST_PROCESSING),
            FlowPhaseEnum.POST_PROCESSING, Set.of(FlowPhaseEnum.QC),
            FlowPhaseEnum.QC, Set.of(FlowPhaseEnum.WAREHOUSE, FlowPhaseEnum.PRINT),
            FlowPhaseEnum.WAREHOUSE, Set.of(FlowPhaseEnum.COMPLETED),
            FlowPhaseEnum.CONFIRM, Set.of(FlowPhaseEnum.COMPLETED),
            FlowPhaseEnum.COMPLETED, Set.of()
    );

    @Override
    public boolean canTransition(Integer fromPhase, Integer toPhase) {
        if (fromPhase == null || toPhase == null) {
            return false;
        }
        FlowPhaseEnum from = FlowPhaseEnum.getByValue(fromPhase);
        FlowPhaseEnum to = FlowPhaseEnum.getByValue(toPhase);
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
        FlowPhaseEnum phase = FlowPhaseEnum.getByValue(currentPhase);
        if (phase == null) {
            return List.of();
        }
        Set<FlowPhaseEnum> allowed = PHASE_TRANSITIONS.getOrDefault(phase, Set.of());
        return allowed.stream().map(FlowPhaseEnum::getValue).toList();
    }

    /**
     * 判断阶段转换是否合法
     *
     * @param fromPhase 当前阶段
     * @param toPhase 目标阶段
     * @param needsPhysicalDelivery 是否需要实体交付（0-不需要，1-需要）
     *                             为 null 时按 1（需要实体交付）处理
     * @return 是否合法
     */
    public static boolean isValidPhaseTransition(
            FlowPhaseEnum fromPhase,
            FlowPhaseEnum toPhase,
            Integer needsPhysicalDelivery) {

        // 已完成状态不可跳转
        if (fromPhase == FlowPhaseEnum.COMPLETED) {
            return false;
        }

        // 不需要实体交付的订单不能进入打印/后处理/质检/仓储阶段
        boolean needsProduction = needsPhysicalDelivery == null || needsPhysicalDelivery == 1;
        if (!needsProduction) {
            if (toPhase == FlowPhaseEnum.PRINT || toPhase == FlowPhaseEnum.POST_PROCESSING
                    || toPhase == FlowPhaseEnum.QC || toPhase == FlowPhaseEnum.WAREHOUSE) {
                return false;
            }
        }

        // 校验阶段转换是否在允许范围内
        Set<FlowPhaseEnum> allowedPhases = PHASE_TRANSITIONS.getOrDefault(fromPhase, Set.of());
        return allowedPhases.contains(toPhase);
    }

    /**
     * 获取指定阶段的下一个默认阶段
     * 用于阶段推进时的自动跳转
     *
     * @param currentPhase 当前阶段
     * @param needsPhysicalDelivery 是否需要实体交付（0-不需要，1-需要）
     *                             为 null 时按 1（需要实体交付）处理
     * @return 下一个阶段，如果没有则返回 null
     */
    public static FlowPhaseEnum getNextPhase(FlowPhaseEnum currentPhase, Integer needsPhysicalDelivery) {
        boolean needsProduction = needsPhysicalDelivery == null || needsPhysicalDelivery == 1;
        return switch (currentPhase) {
            case ORDER -> FlowPhaseEnum.DESIGN;
            case DESIGN -> needsProduction ? FlowPhaseEnum.PRINT : FlowPhaseEnum.CONFIRM;
            case PRINT -> FlowPhaseEnum.POST_PROCESSING;
            case POST_PROCESSING -> FlowPhaseEnum.QC;
            case QC -> FlowPhaseEnum.WAREHOUSE;
            case WAREHOUSE -> FlowPhaseEnum.COMPLETED;
            case CONFIRM -> FlowPhaseEnum.COMPLETED;
            case COMPLETED -> null;
        };
    }

    /**
     * 阶段推进结果
     * 封装推进到的目标阶段及进入该阶段后的初始状态
     */
    public record PhaseAndStatus(FlowPhaseEnum phase, FlowStatusEnum initialStatus) {}

    /**
     * 根据当前阶段、目标状态决策下一阶段及初始状态
     *
     * 【核心修复 P1-3】：阶段推进时，必须同时确定初始可见状态
     * - DATA_AUDIT_PASSED → DESIGN + PENDING_DESIGN
     * - DESIGN_REVIEW_PASSED → PRINT + PENDING_PRINT 或 CONFIRM + AWAITING_CONFIRM
     *
     * 阶段推进规则：
     * - DATA_AUDIT_PASSED(1030) → 进入 DESIGN(20)，status 变为 PENDING_DESIGN(2010)
     * - DESIGN_REVIEW_PASSED(2050) → 进入 PRINT(30) 或 CONFIRM(70)
     * - PRINT_COMPLETED(3030) → 进入 POST_PROCESSING(40)
     * - QC_PASSED(5020) → 进入 WAREHOUSE(60)
     * - WAREHOUSED(6020) → 进入 COMPLETED(80)
     * - COMPLETED(8010) → 进入 COMPLETED(80)
     *
     * @param currentPhase 当前阶段
     * @param targetStatus 动作触发的目标状态
     * @param action 触发动作
     * @param needsPhysicalDelivery 是否需要实体交付
     * @return 阶段推进结果，如果不需要推进则 phase=null
     */
    public static PhaseAndStatus decideNextPhaseAndStatus(
            FlowPhaseEnum currentPhase,
            FlowStatusEnum targetStatus,
            FlowActionEnum action,
            Integer needsPhysicalDelivery,
            Integer orderType) {

        boolean needsProduction = needsPhysicalDelivery == null || needsPhysicalDelivery == 1;

        // 审核通过 → 进入设计阶段，初始状态为 PENDING_DESIGN（待分配设计师）
        if (targetStatus == FlowStatusEnum.DATA_AUDIT_PASSED) {
            return new PhaseAndStatus(FlowPhaseEnum.DESIGN, FlowStatusEnum.PENDING_DESIGN);
        }

        // 设计完成 → 根据 needsPhysicalDelivery 决定下一阶段
        if (targetStatus == FlowStatusEnum.DESIGN_COMPLETED) {
            boolean needsPhysical = needsPhysicalDelivery == null || needsPhysicalDelivery == 1;
            if (needsPhysical) {
                return new PhaseAndStatus(FlowPhaseEnum.PRINT, FlowStatusEnum.PENDING_PRINT);
            } else {
                return new PhaseAndStatus(FlowPhaseEnum.CONFIRM, FlowStatusEnum.AWAITING_CONFIRM);
            }
        }

        // 打印完成 → 医疗器械进入后处理，非医疗器械直接进入质检
        // orderType: 1=医疗器械，2=非医疗器械；null 时按医疗器械处理
        if (targetStatus == FlowStatusEnum.PRINT_COMPLETED) {
            boolean isMedical = !Integer.valueOf(2).equals(orderType);
            if (isMedical) {
                return new PhaseAndStatus(FlowPhaseEnum.POST_PROCESSING, FlowStatusEnum.POST_PROCESSING);
            } else {
                return new PhaseAndStatus(FlowPhaseEnum.QC, FlowStatusEnum.QC_IN_PROGRESS);
            }
        }

        // 后处理完成 → 进入质检阶段，初始状态为 QC_IN_PROGRESS
        if (targetStatus == FlowStatusEnum.QC_IN_PROGRESS
                && action == FlowActionEnum.COMPLETE_POST_PROCESSING) {
            return new PhaseAndStatus(FlowPhaseEnum.QC, FlowStatusEnum.QC_IN_PROGRESS);
        }

        // 质检合格 → 进入仓储阶段，初始状态为 WAREHOUSE_IN
        if (targetStatus == FlowStatusEnum.QC_PASSED) {
            return new PhaseAndStatus(FlowPhaseEnum.WAREHOUSE, FlowStatusEnum.WAREHOUSE_IN);
        }

        // 已入库 → 进入已完成
        if (targetStatus == FlowStatusEnum.WAREHOUSED) {
            return new PhaseAndStatus(FlowPhaseEnum.COMPLETED, FlowStatusEnum.COMPLETED);
        }

        // 客户确认 → 进入已完成
        if (targetStatus == FlowStatusEnum.COMPLETED) {
            return new PhaseAndStatus(FlowPhaseEnum.COMPLETED, FlowStatusEnum.COMPLETED);
        }

        // 质检不合格回退到打印 → 回退到打印阶段，初始状态为 PENDING_PRINT
        if (action == FlowActionEnum.REWORK_TO_PRINT) {
            return new PhaseAndStatus(FlowPhaseEnum.PRINT, FlowStatusEnum.PENDING_PRINT);
        }

        // 其他状态不推进阶段
        return new PhaseAndStatus(null, null);
    }

    /**
     * 判断目标状态是否为不可见状态
     * 不可见状态会被状态机内部吸收，不会落库
     *
     * @param status 目标状态
     * @return true-不可见状态，false-正常状态
     */
    public static boolean isInvisibleStatus(FlowStatusEnum status) {
        // DESIGN_REVIEW_PASSED 改为可见状态，订单停留在此状态直到下载数据包
        return false;
    }

    /**
     * 判断当前动作是否会触发阶段推进
     * 用于日志记录和业务判断
     *
     * @param action 动作枚举
     * @return true-会触发阶段推进，false-仅状态变化
     */
    public static boolean isPhaseChangeAction(FlowActionEnum action) {
        return switch (action) {
            case DATA_AUDIT_PASS, COMPLETE_DESIGN, COMPLETE_PRINT,
                 COMPLETE_POST_PROCESSING, QC_PASS, REWORK_COMPLETE,
                 COMPLETE_WAREHOUSE_IN, USER_CONFIRM, REWORK_TO_PRINT -> true;
            default -> false;
        };
    }
}
