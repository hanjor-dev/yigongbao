package com.yigongbao.framework.annotation;

import java.lang.annotation.*;

/**
 * 请求签名验证注解
 * 标注此注解的接口需要携带签名 Header：X-App-Key、X-Timestamp、X-Nonce、X-Signature
 *
 * @author hanjor
 * @date 2026-05-08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSign {
}
