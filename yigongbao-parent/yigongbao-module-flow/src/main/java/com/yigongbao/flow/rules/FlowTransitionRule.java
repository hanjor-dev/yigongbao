package com.yigongbao.flow.rules;

import java.util.List;

/**
 * 阶段转换规则接口
 * 定义阶段流转的通用规则，供各阶段模块实现
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface FlowTransitionRule {

    /**
     * 判断是否允许从当前阶段流转到目标阶段
     *
     * @param fromPhase 当前阶段值
     * @param toPhase 目标阶段值
     * @return true-允许，false-不允许
     */
    boolean canTransition(Integer fromPhase, Integer toPhase);

    /**
     * 获取当前阶段可流转的下一阶段列表
     *
     * @param currentPhase 当前阶段值
     * @return 可流转的阶段列表
     */
    List<Integer> getAvailableNextPhases(Integer currentPhase);
}
