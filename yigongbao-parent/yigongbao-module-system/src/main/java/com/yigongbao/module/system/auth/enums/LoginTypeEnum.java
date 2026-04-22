package com.yigongbao.module.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录类型枚举
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    /** 账号密码登录 */
    PASSWORD("PASSWORD"),

    /** 手机验证码登录 */
    PHONE("PHONE"),

    /** 邮箱验证码登录 */
    EMAIL("EMAIL");

    private final String value;
}
