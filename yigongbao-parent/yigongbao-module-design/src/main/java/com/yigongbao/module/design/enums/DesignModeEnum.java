package com.yigongbao.module.design.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设计模式枚举
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Getter
@AllArgsConstructor
public enum DesignModeEnum {

    /**
     * 线下修改
     */
    OFFLINE(1, "线下修改"),

    /**
     * 在线编辑
     */
    ONLINE(2, "在线编辑");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static DesignModeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DesignModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return null;
    }
}
