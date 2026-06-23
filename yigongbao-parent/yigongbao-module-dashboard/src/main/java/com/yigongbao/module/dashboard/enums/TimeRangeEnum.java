package com.yigongbao.module.dashboard.enums;

import lombok.Getter;

/**
 * 时间范围枚举
 */
@Getter
public enum TimeRangeEnum {
    TODAY("today", "今日"),
    WEEK("week", "本周"),
    MONTH("month", "本月"),
    QUARTER("quarter", "本季度"),
    YEAR("year", "本年"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String desc;

    TimeRangeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TimeRangeEnum fromCode(String code) {
        for (TimeRangeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("无效的时间范围: " + code);
    }
}
