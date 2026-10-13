package com.yigongbao.module.design.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审核结果枚举
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Getter
@AllArgsConstructor
public enum ReviewResultEnum {

    /**
     * 驳回
     */
    REJECT(0, "驳回"),

    /**
     * 通过
     */
    PASS(1, "通过");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static ReviewResultEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewResultEnum result : values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        return null;
    }
}
