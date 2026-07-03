package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单类型枚举
 *
 * @author hanjor
 * @date 2026-07-03
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    /**
     * 医疗器械订单
     */
    MEDICAL_DEVICE(1, "医疗器械"),

    /**
     * 非医疗器械订单
     */
    NON_MEDICAL_DEVICE(2, "非医疗器械");

    /**
     * 枚举值，对应数据库中的字段值
     */
    private final Integer value;

    /**
     * 枚举描述
     */
    private final String desc;

    public static OrderTypeEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderTypeEnum item : OrderTypeEnum.values()) {
            if (item.getValue().equals(value)) {
                return item;
            }
        }
        return null;
    }

    public boolean isMedicalDevice() {
        return this == MEDICAL_DEVICE;
    }

    public boolean isNonMedicalDevice() {
        return this == NON_MEDICAL_DEVICE;
    }
}
