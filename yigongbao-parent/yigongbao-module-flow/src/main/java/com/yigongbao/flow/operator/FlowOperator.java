package com.yigongbao.flow.operator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程操作人信息
 * 封装执行动作时的操作人信息，避免散列参数传递
 *
 * 【字段说明】
 * - operatorId：操作人ID（必填，用于记录操作履历）
 * - operatorName：操作人姓名（可选，用于历史展示）
 * - remark：备注（如驳回原因，可选）
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowOperator {

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 备注（如驳回原因）
     */
    private String remark;

    /**
     * 创建仅有ID和姓名的操作人对象
     *
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @return FlowOperator 对象
     */
    public static FlowOperator of(Long operatorId, String operatorName) {
        return new FlowOperator(operatorId, operatorName, null);
    }
}
