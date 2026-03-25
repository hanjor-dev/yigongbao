package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 编码重置类型枚举
 * 定义编码序号的重置策略
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Getter
@AllArgsConstructor
public enum CodeResetTypeEnum {

    DAY("DAY", "每日重置"),
    MONTH("MONTH", "每月重置"),
    YEAR("YEAR", "每年重置"),
    NEVER("NEVER", "不重置");

    /**
     * 重置类型编码
     */
    private final String code;

    /**
     * 重置类型名称
     */
    private final String name;
}
