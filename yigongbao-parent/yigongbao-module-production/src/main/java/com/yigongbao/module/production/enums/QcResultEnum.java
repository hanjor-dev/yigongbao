package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质检结果枚举
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
public enum QcResultEnum {
    PASS("pass", "合格"),
    FAIL("fail", "不合格");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
