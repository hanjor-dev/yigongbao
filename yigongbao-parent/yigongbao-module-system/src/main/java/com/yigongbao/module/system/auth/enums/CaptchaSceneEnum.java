package com.yigongbao.module.system.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码使用场景枚举，用于隔离 Redis key 前缀，防止跨场景复用
 *
 * @author hanjor
 * @date 2026-04-22
 */
@Getter
@AllArgsConstructor
public enum CaptchaSceneEnum {

    /** 登录场景 */
    LOGIN("login"),

    /** 忘记密码场景 */
    FORGOT("forgot");

    private final String scene;
}
