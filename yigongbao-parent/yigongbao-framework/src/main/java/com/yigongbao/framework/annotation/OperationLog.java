package com.yigongbao.framework.annotation;

import com.yigongbao.common.enums.OperationTypeEnum;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名称
     * 如：用户管理、订单管理
     */
    String module();

    /**
     * 业务类型
     * 如：CREATE、UPDATE、DELETE
     */
    OperationTypeEnum businessType();

    /**
     * 操作描述
     * 如：创建用户、审核订单
     */
    String operation();

    /**
     * 业务描述（可选）
     * 支持 SpEL 表达式，用于记录具体业务信息
     * 如：#{userName} 管理员创建了用户 #{userName}
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录返回结果
     */
    boolean logResult() default false;
}
