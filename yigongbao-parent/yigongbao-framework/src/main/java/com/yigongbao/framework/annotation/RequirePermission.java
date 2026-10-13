package com.yigongbao.framework.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 用于标记需要校验权限的接口
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限编码
     * 如：system:user:add
     */
    String value();
}
