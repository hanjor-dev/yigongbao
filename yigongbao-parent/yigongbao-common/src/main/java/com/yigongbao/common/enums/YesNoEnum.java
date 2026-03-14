package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 是否枚举
 * 用于表示布尔值的场景，如是否启用、是否删除、是否确认等
 *
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Getter
@AllArgsConstructor
public enum YesNoEnum {

    /**
     * 是
     */
    YES(1, "是"),

    /**
     * 否
     */
    NO(0, "否");

    /**
     * 枚举值，对应数据库中的字段值
     */
    private final Integer value;

    /**
     * 枚举描述，用于显示
     */
    private final String desc;

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static YesNoEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (YesNoEnum enumItem : YesNoEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 判断是否为"是"
     *
     * @return true 表示是，false 表示否
     */
    public boolean isYes() {
        return this == YES;
    }

    /**
     * 判断是否为"否"
     *
     * @return true 表示否，false 表示是
     */
    public boolean isNo() {
        return this == NO;
    }

}
