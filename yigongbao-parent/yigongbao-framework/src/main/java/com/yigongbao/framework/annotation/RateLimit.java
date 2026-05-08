package com.yigongbao.framework.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * 基于 Redis 固定窗口算法，支持按 IP 或用户ID 维度限流
 *
 * @author hanjor
 * @date 2026-05-08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内最大请求数，默认 60
     */
    int limit() default 60;

    /**
     * 时间窗口（秒），默认 60
     */
    int window() default 60;

    /**
     * 限流维度：IP 按客户端IP；USER 已登录按用户ID，未登录降级为IP
     */
    Dimension dimension() default Dimension.USER;

    enum Dimension {
        IP,
        USER
    }
}
