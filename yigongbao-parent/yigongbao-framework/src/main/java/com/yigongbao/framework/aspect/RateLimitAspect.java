package com.yigongbao.framework.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.framework.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 限流切面
 * 基于 Redis 固定窗口算法，对标注 @RateLimit 的接口进行频率限制
 *
 * @author hanjor
 * @date 2026-05-08
 */
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Lua 脚本：固定窗口计数，INCR + EXPIRE 原子操作
     * KEYS[1]: Redis key
     * ARGV[1]: 窗口大小（秒）
     * ARGV[2]: 最大请求数
     * 返回 1 表示允许，0 表示超限
     */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local c=redis.call('INCR',KEYS[1]) " +
            "if c==1 then redis.call('EXPIRE',KEYS[1],tonumber(ARGV[1])) end " +
            "if c>tonumber(ARGV[2]) then return 0 end return 1",
            Long.class);

    @Around("@annotation(rateLimit)")
    public Object limit(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getRequest();
        String key = buildKey(rateLimit, request);
        Long result;
        try {
            result = redisTemplate.execute(RATE_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(rateLimit.window()),
                    String.valueOf(rateLimit.limit()));
        } catch (Exception e) {
            // Redis 不可用时 fail-open，避免全站不可用
            log.warn("限流 Redis 异常，降级放行，key={}", key, e);
            return point.proceed();
        }
        if (result == null || result == 0L) {
            log.warn("限流触发，key={}", key);
            throw new BusinessException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED);
        }
        return point.proceed();
    }

    /**
     * 构建 Redis key
     * USER 维度：已登录用 rate:user:{userId}:{uri}，未登录降级为 IP
     * IP 维度：rate:ip:{ip}:{uri}
     */
    private String buildKey(RateLimit rateLimit, HttpServletRequest request) {
        String uri = request.getRequestURI().replace("/", "_");
        if (rateLimit.dimension() == RateLimit.Dimension.USER) {
            try {
                if (StpUtil.isLogin()) {
                    return "rate:user:" + StpUtil.getLoginIdAsLong() + ":" + uri;
                }
            } catch (Exception ignored) {
            }
        }
        return "rate:ip:" + getClientIp(request) + ":" + uri;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
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
