package com.yigongbao.framework.aspect;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.framework.annotation.RequireSign;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 请求签名验证切面
 * 对标注 @RequireSign 的接口进行签名验证，防止参数篡改和重放攻击
 *
 * <p>签名算法：MD5(appKey + timestamp + nonce + appSecret)
 * <p>验证流程：Header 存在性 → 时间窗口（5分钟）→ nonce 防重放（Redis SET NX）→ 签名比对
 *
 * @author hanjor
 * @date 2026-05-08
 */
@Aspect
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SignAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.sign.secret}")
    private String appSecret;

    /** 时间窗口：5 分钟（毫秒） */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    @Around("@annotation(com.yigongbao.framework.annotation.RequireSign)")
    public Object verify(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = getRequest();

        // 1. 读取并校验 Header 存在性
        String appKey    = requireHeader(request, "X-App-Key");
        String timestamp = requireHeader(request, "X-Timestamp");
        String nonce     = requireHeader(request, "X-Nonce");
        String signature = requireHeader(request, "X-Signature");

        // 2. 时间窗口校验（timestamp 为秒级）
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCodeEnum.SIGN_PARAM_MISSING);
        }
        if (Math.abs(System.currentTimeMillis() - ts * 1000L) > WINDOW_MS) {
            throw new BusinessException(ErrorCodeEnum.SIGN_TIMESTAMP_EXPIRED);
        }

        // 3. nonce 防重放（SET NX，TTL 5min）
        String nonceKey = "sign:nonce:" + nonce;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(nonceKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isNew)) {
            throw new BusinessException(ErrorCodeEnum.SIGN_NONCE_USED);
        }

        // 4. 签名比对
        String expected = DigestUtils.md5DigestAsHex(
                (appKey + timestamp + nonce + appSecret).getBytes(StandardCharsets.UTF_8));
        if (!expected.equalsIgnoreCase(signature)) {
            // 签名失败删除 nonce，允许客户端修正参数后重试
            redisTemplate.delete(nonceKey);
            log.warn("签名验证失败，appKey={}, uri={}", appKey, request.getRequestURI());
            throw new BusinessException(ErrorCodeEnum.SIGN_INVALID);
        }

        return point.proceed();
    }

    private String requireHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null || v.isBlank()) {
            throw new BusinessException(ErrorCodeEnum.SIGN_PARAM_MISSING);
        }
        return v;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
