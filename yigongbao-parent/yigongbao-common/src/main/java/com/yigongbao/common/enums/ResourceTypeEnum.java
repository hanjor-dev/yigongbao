package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 资源类型枚举
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {

    MENU_FIRST(1, "一级菜单"),
    MENU_SECOND(2, "二级菜单"),
    BUTTON(3, "按钮");

    /**
     * 类型编码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 类型编码
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static ResourceTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否为菜单类型（一级或二级）
     *
     * @param code 类型编码
     * @return true-是菜单，false-不是
     */
    public static boolean isMenu(Integer code) {
        return MENU_FIRST.getCode().equals(code) || MENU_SECOND.getCode().equals(code);
    }
}
