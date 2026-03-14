package com.yigongbao.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 统一响应拦截器
 * 为所有接口统一添加时间戳，并处理返回值包装
 *
 * @author hanjor
 * @date 2026-03-14 15:00:00
 */
@Component
public class ResultInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ResultInterceptor.class);

    /**
     * 请求处理之前调用
     * 记录请求开始时间
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        // 记录请求日志
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("请求开始：{} {}", method, uri);

        return true;
    }

    /**
     * 请求处理之后、渲染视图之前调用
     * 可以对响应进行处理
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           org.springframework.web.servlet.ModelAndView modelAndView) {
        // 此处可以添加统一视图处理逻辑
    }

    /**
     * 请求处理完全完成后调用
     * 记录请求耗时日志
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        // 计算请求耗时
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime != null) {
            long endTime = System.currentTimeMillis();
            long executeTime = endTime - startTime;

            String uri = request.getRequestURI();
            String method = request.getMethod();

            // 慢请求日志警告
            if (executeTime > 1000) {
                log.warn("慢请求：{} {} 耗时 {}ms", method, uri, executeTime);
            } else {
                log.debug("请求完成：{} {} 耗时 {}ms", method, uri, executeTime);
            }
        }

        // 记录异常
        if (ex != null) {
            log.error("请求异常：{} {}", request.getRequestURI(), ex.getMessage(), ex);
        }
    }
}
