package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单阶段枚举
 * 定义订单的流转阶段
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderPhaseEnum {

    /**
     * 订单阶段
     */
    ORDER(1, "订单"),

    /**
     * 设计阶段
     */
    DESIGN(2, "设计"),

    /**
     * 生产阶段
     */
    PRODUCTION(3, "生产");

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
    public static OrderPhaseEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderPhaseEnum enumItem : OrderPhaseEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }
}
