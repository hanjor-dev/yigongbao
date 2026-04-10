package com.yigongbao.flow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流转阶段枚举
 * 定义业务流转的阶段
 *
 * 【编码规则】
 * - Phase value 使用间隔10的整数：10, 20, 30...
 * - 间隔设计允许在任意两个已有阶段之间插入新阶段，不影响已有值和历史数据
 * - 例如：在 DESIGN(20) 和 PRINT(30) 之间可插入 LAYOUT(25)
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum FlowPhaseEnum {

    /**
     * 订单阶段
     */
    ORDER(10, "订单阶段"),

    /**
     * 设计阶段
     */
    DESIGN(20, "设计阶段"),

    /**
     * 打印阶段
     */
    PRINT(30, "打印阶段"),

    /**
     * 后处理阶段
     */
    POST_PROCESSING(40, "后处理阶段"),

    /**
     * 质检阶段
     */
    QC(50, "质检阶段"),

    /**
     * 仓储阶段
     */
    WAREHOUSE(60, "仓储阶段"),

    /**
     * 确认阶段
     */
    CONFIRM(70, "确认阶段"),

    /**
     * 已完成
     */
    COMPLETED(80, "已完成");

    /**
     * 阶段值
     */
    private final Integer value;

    /**
     * 阶段名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param value 阶段值
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static FlowPhaseEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (FlowPhaseEnum enumItem : FlowPhaseEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }
}
