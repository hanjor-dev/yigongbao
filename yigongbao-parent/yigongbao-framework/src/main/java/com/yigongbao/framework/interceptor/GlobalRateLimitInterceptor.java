package com.yigongbao.framework.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 全局限流拦截器
 * 对所有接口应用默认限流兜底，限流值通过 app.rate-limit.* 配置
 *
 * @author hanjor
 * @date 2026-05-08
 */
@RequiredArgsConstructor
@Slf4j
public class GlobalRateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    /** 默认每分钟最大请求数 */
    private final int defaultLimit;
    /** 默认时间窗口（秒） */
    private final int defaultWindow;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = buildKey(request);
        Long result;
        try {
            // 将参数直接嵌入脚本，避免 RedisTemplate varargs 序列化问题导致 ARGV 为 nil
            String script = String.format(
                    "local c=redis.call('INCR',KEYS[1]) " +
                    "if c==1 then redis.call('EXPIRE',KEYS[1],%d) end " +
                    "if c>=%d then return 0 end return 1",
                    defaultWindow, defaultLimit);
            result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(key));
        } catch (Exception e) {
            log.warn("全局限流 Redis 异常，降级放行，key={}", key, e);
            return true;
        }
        if (result == null || result == 0L) {
            log.warn("全局限流触发，key={}", key);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error(ErrorCodeEnum.RATE_LIMIT_EXCEEDED.getCode(),
                            ErrorCodeEnum.RATE_LIMIT_EXCEEDED.getMessage())));
            return false;
        }
        return true;
    }

    private String buildKey(HttpServletRequest request) {
        String uri = request.getRequestURI().replace("/", "_");
        try {
            if (StpUtil.isLogin()) {
                return "rate:user:" + StpUtil.getLoginIdAsLong() + ":" + uri;
            }
        } catch (Exception ignored) {
        }
        return "rate:ip:" + getClientIp(request) + ":" + uri;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
