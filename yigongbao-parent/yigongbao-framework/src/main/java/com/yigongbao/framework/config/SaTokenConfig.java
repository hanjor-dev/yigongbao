package com.yigongbao.framework.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * 配置认证拦截器，实现接口权限认证
 *
 * @author hanjor
 * @date 2026-03-14 15:00:00
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器
     * 拦截所有请求，进行登录认证
     *
     * @param registry 注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，拦截所有请求
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
            .addPathPatterns("/**")
            // 放行路径：静态资源、登录接口、注册接口等
            .excludePathPatterns(
                // 静态资源
                "/static/**",
                "/favicon.ico",
                // 公共接口（根据实际情况调整）
                "/common/**",
                // 错误页面
                "/error",
                "/test/**"
            );
    }
}
