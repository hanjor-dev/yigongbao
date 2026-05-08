package com.yigongbao.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.framework.interceptor.GlobalRateLimitInterceptor;
import com.yigongbao.framework.interceptor.ResultInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * Web MVC 配置类
 * 配置 URL 匹配策略、拦截器等 Web 相关功能
 *
 * @author hanjor
 * @date 2026-03-14 14:30:00
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ResultInterceptor resultInterceptor;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.default-limit:60}")
    private int defaultLimit;

    @Value("${app.rate-limit.default-window:60}")
    private int defaultWindow;

    public WebMvcConfig(ResultInterceptor resultInterceptor,
                        RedisTemplate<String, Object> redisTemplate,
                        ObjectMapper objectMapper) {
        this.resultInterceptor = resultInterceptor;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 配置 URL 路径匹配策略
     * 使用 ant_path_matcher 匹配策略，支持 /api/user/** 这种路径
     *
     * @param configurer PathMatchConfigurer 实例
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 配置路径匹配策略，使用 Ant 风格的路径匹配器
        // 这种策略比默认的 AntPathMatcher 更严格，但性能更好
        configurer.setUseSuffixPatternMatch(false);
        // 配置是否使用后缀匹配（如 .html、.json 等）
        // 设为 false 可以让 /api/users 同时匹配 /api/users 和 /api/users.json
    }

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局限流拦截器（兜底，所有接口生效）
        registry.addInterceptor(new GlobalRateLimitInterceptor(redisTemplate, objectMapper, defaultLimit, defaultWindow))
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/static/**", "/favicon.ico", "/error",
                "/doc.html", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**"
            );

        // 注册统一响应拦截器
        registry.addInterceptor(resultInterceptor)
            .addPathPatterns("/**")
            // 放行路径（拦截器路径相对于 context-path，不需要加 /api 前缀）
            .excludePathPatterns(
                "/static/**",
                "/favicon.ico",
                "/error",
                // Swagger / OpenAPI 文档（用于导入 Apifox）
                "/doc.html",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/webjars/**"
            );
    }

    /**
     * 配置跨域映射
     * 允许前端应用跨域访问 API
     *
     * @param registry 跨域注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            // 允许的来源域名，生产环境应限制具体域名
            .allowedOriginPatterns("*")
            // 允许的请求方法
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            // 允许的请求头
            .allowedHeaders("*")
            // 是否允许携带凭证（cookies、authorization header）
            .allowCredentials(true)
            // 预检请求的缓存时间（秒）
            .maxAge(3600);
    }

}
