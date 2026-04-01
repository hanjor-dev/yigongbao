package com.yigongbao.flow.result;

import lombok.Data;

/**
 * 阶段推进结果
 * 封装状态变更后的 phase 和 status 变更信息
 *
 * 设计说明：
 * - targetStatus: 动作直接触发的目标状态（由 getTargetStatus 返回）
 * - initialStatus: 进入新阶段后的初始可见状态（由 decideInitialStatus 计算）
 * - 当 phase 不推进时（phaseChanged=false）：targetPhase=null，直接使用 targetStatus
 * - 当 phase 推进时（phaseChanged=true）：
 *   - DB 落库使用 initialStatus（初始可见状态）
 *   - targetStatus 作为动作触发的中间状态，仅用于历史记录
 * - 【重要】phase 和 status 始终成对出现（phase=1 对应 ORDER 阶段状态，phase=2 对应 DESIGN 阶段状态）
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class TransitionResult {

    /**
     * 目标阶段
     * 可能是当前阶段（不变），也可能是下一个阶段（推进后）
     */
    private Integer targetPhase;

    /**
     * 目标状态
     * 落库的状态值（推进时为 initialStatus，不推进时为 targetStatus）
     */
    private Integer targetStatus;

    /**
     * 进入新阶段后的初始可见状态
     * 仅当 phase 推进时有意义，否则为 null
     * 例如：DATA_AUDIT_PASS 后进入 DESIGN 阶段，initialStatus=DESIGNING(21)
     */
    private Integer initialStatus;

    /**
     * 是否发生了阶段推进
     * true - phase 已推进到新阶段，targetStatus 为中间状态，initialStatus 为落库状态
     * false - phase 保持不变，targetStatus 即为落库状态
     */
    private boolean phaseChanged;

    /**
     * 创建状态不变的结果
     *
     * @param currentPhase 当前阶段
     * @param targetStatus 目标状态
     * @return 阶段推进结果
     */
    public static TransitionResult of(Integer currentPhase, Integer targetStatus) {
        TransitionResult result = new TransitionResult();
        result.setTargetPhase(currentPhase);
        result.setTargetStatus(targetStatus);
        result.setInitialStatus(null);
        result.setPhaseChanged(false);
        return result;
    }

    /**
     * 创建阶段推进的结果
     *
     * @param newPhase 新的阶段值
     * @param targetStatus 动作触发的中间状态（用于历史记录）
     * @param initialStatus 进入新阶段后的初始可见状态（用于落库）
     * @return 阶段推进结果
     */
    public static TransitionResult ofWithPhaseChange(Integer newPhase, Integer targetStatus, Integer initialStatus) {
        TransitionResult result = new TransitionResult();
        result.setTargetPhase(newPhase);
        result.setTargetStatus(targetStatus);
        result.setInitialStatus(initialStatus);
        result.setPhaseChanged(true);
        return result;
    }

    /**
     * 获取最终落库的状态值
     * - 如果 phase 推进了，返回 initialStatus（新阶段的初始状态）
     * - 如果 phase 未推进，返回 targetStatus（当前状态）
     */
    public Integer getFinalStatus() {
        return phaseChanged ? initialStatus : targetStatus;
    }
}
