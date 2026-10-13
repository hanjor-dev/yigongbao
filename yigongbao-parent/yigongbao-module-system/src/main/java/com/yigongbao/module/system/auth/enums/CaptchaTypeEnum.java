package com.yigongbao.module.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码类型枚举（不含 PASSWORD，仅用于验证码发送/校验场景）
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Getter
@AllArgsConstructor
public enum CaptchaTypeEnum {

    /** 手机验证码 */
    PHONE("PHONE"),

    /** 邮箱验证码 */
    EMAIL("EMAIL");

    private final String value;
}
