package com.yigongbao.framework.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * 配置认证拦截器，实现接口权限认证
 * <p>
 * 说明：
 * Sa-Token 有两套认证机制：Servlet Filter 和 Spring MVC Interceptor
 * - SaServletFilter：Servlet 过滤器，在请求进入 Spring MVC 之前拦截（@Order(-100)，最高优先级）
 * - SaInterceptor：Spring MVC 拦截器，在 Controller 层面拦截
 * 两套机制独立工作，必须同时配置排除路径，否则 Servlet Filter 会在 Interceptor 之前拦截请求
 *
 * @author hanjor
 * @date 2026-03-14 15:00:00
 */
@Configuration
@ConditionalOnProperty(name = "satoken.interceptor.enable", havingValue = "true", matchIfMissing = true)
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 过滤器（Servlet Filter）
     * 只负责路径过滤，不执行认证检查（认证由 Interceptor 负责）
     * 主要用于排除 WebSocket 等特殊请求
     */
    @Bean
    @Order(-100)
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/static/**")
                .addExclude("/favicon.ico")
                .addExclude("/common/**")
                .addExclude("/error")
                .addExclude("/test/**")
                .addExclude("/system/auth/login")
                .addExclude("/system/auth/logout")
                .addExclude("/system/auth/info")
                .addExclude("/system/auth/password")
                .addExclude("/system/auth/captcha")
                .addExclude("/system/auth/forgot-password/**")
                .addExclude("/image-captch/**")
                .addExclude("/doc.html")
                .addExclude("/swagger-ui/**")
                .addExclude("/swagger-ui.html")
                .addExclude("/v3/api-docs/**")
                .addExclude("/webjars/**")
                .addExclude("/files/public/**")
                .addExclude("/system/config/key/**")
                .addExclude("/ws/device")
                .addExclude("/notification/websocket")
                .addExclude("/basic/processing-center/**")
                .addExclude("/basic/device/**")
                .addExclude("/imaging/v1/dcm")
                .addExclude("/imaging/v1/stl")
                .addExclude("/imaging/v1/mark");
                // 注意：不设置 setAuth()，Filter 只做路径过滤，认证由 Interceptor 负责
    }

    /**
     * 注册 Sa-Token 拦截器
     * 拦截所有请求，进行登录认证
     *
     * @param registry 注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        var registration = registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()));
        registration.addPathPatterns("/**");

        // 放行路径：静态资源、登录接口、注册接口等
        // 注意：拦截器 excludePathPatterns 相对于 context-path，不需要加 /api 前缀
        registration.excludePathPatterns(
                // 静态资源
                "/static/**",
                "/favicon.ico",
                // 公共接口
                "/common/**",
                // 错误页面
                "/error",
                "/test/**",
                // 认证接口
                "/system/auth/login",
                "/system/auth/logout",
                "/system/auth/info",
                "/system/auth/password",
                "/system/auth/captcha",
                "/system/auth/forgot-password/**",
                // 图形验证码接口（无需登录）
                "/image-captch/**",
                // Swagger/OpenAPI 文档（用于导入 Apifox）
                "/doc.html",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/webjars/**",
                // 文件访问接口（静态资源，无需登录）
                "/files/public/**",
                // 登录验证码冷却时间配置
                "/system/config/key/**",
                // 设备 WebSocket（允许任何客户端连接，无需身份认证）
                "/ws/device",
                // 通知 WebSocket（握手阶段无 token，认证通过首帧 AUTH 消息完成）
                "/notification/websocket",
                // 基础数据加工中心和设备接口（无需登录）
                "/basic/processing-center/**",
                "/basic/device/**",
                // 影像查看器适配接口（二维码传播，无需登录）
                "/imaging/v1/dcm",
                "/imaging/v1/stl",
                "/imaging/v1/mark"
        );
    }
}
