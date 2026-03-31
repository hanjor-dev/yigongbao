package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单类型枚举
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    /**
     * 医疗器械
     */
    MEDICAL_DEVICE(1, "医疗器械"),

    /**
     * 非医疗器械
     */
    NON_MEDICAL_DEVICE(2, "非医疗器械"),

    /**
     * 服务
     */
    SERVICE(3, "服务");

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 类型名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param value 类型值
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static OrderTypeEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderTypeEnum enumItem : OrderTypeEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }
}
