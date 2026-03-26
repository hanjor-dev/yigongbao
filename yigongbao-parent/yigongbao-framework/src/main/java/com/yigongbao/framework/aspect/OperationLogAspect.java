package com.yigongbao.framework.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.service.OperationLogService;
import com.yigongbao.framework.annotation.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 * 拦截标注 @OperationLog 的方法，记录操作日志
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 敏感字段脱敏正则
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "\"(password|passwd|secret|token|key|证书密码)\"?:\\s*\"[^\"]+\"",
            Pattern.CASE_INSENSITIVE);

    /**
     * 敏感字段列表（用于参数名判断）
     */
    private static final String[] SENSITIVE_PARAMS = {
            "password", "passwd", "oldPassword", "newPassword", "secret", "token", "key"
    };

    @Around("@annotation(com.yigongbao.framework.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String requestUrl = request != null ? request.getRequestURI() : "";
        String requestMethod = request != null ? request.getMethod() : "";
        String requestIp = getClientIp(request);

        // 获取当前用户
        Long userId = null;
        String username = "-";
        String realName = "-";
        try {
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsLong();
                username = getUsername(userId);
                realName = getRealName(userId);
            }
        } catch (Exception e) {
            log.debug("获取登录用户信息失败", e);
        }

        // 获取 User-Agent
        String userAgent = getRequest() != null ? getRequest().getHeader("User-Agent") : null;

        // 获取请求参数（脱敏）
        String requestParams = null;
        if (annotation.logParams()) {
            requestParams = getRequestParams(point, annotation);
        }

        // 执行目标方法
        boolean success = true;
        Object result = null;
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            success = false;
            // 异常时无论 logParams 是否开启，都获取请求参数以便排查问题
            String errorRequestParams = requestParams;
            if (errorRequestParams == null) {
                errorRequestParams = getRequestParams(point, annotation);
            }
            long costTime = System.currentTimeMillis() - startTime;
            saveLogAsync(annotation, requestUrl, requestMethod, requestIp,
                    userId, username, realName, errorRequestParams, userAgent, costTime, 0, e.getMessage());
            throw e;
        } finally {
            if (success) {
                long costTime = System.currentTimeMillis() - startTime;
                saveLogAsync(annotation, requestUrl, requestMethod, requestIp,
                        userId, username, realName, requestParams, userAgent, costTime, 1, null);
            }
        }
    }

    /**
     * 异步保存日志
     */
    @Async
    public void saveLogAsync(OperationLog annotation, String requestUrl,
            String requestMethod, String requestIp, Long userId, String username, String realName,
            String requestParams, String userAgent, long costTime, Integer status, String errorMessage) {
        try {
            operationLogService.saveLog(
                    annotation.businessType(),
                    annotation.module(),
                    annotation.description() + " - " + requestUrl,
                    annotation.operation(),
                    userId,
                    realName,
                    username,
                    requestIp,
                    userAgent,
                    requestMethod,
                    costTime,
                    status == 1,
                    errorMessage,
                    requestParams
            );
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 脱敏处理
     */
    private String maskSensitiveData(String json) {
        if (json == null) {
            return null;
        }
        return SENSITIVE_PATTERN.matcher(json).replaceAll("\"$1\":\"***\"");
    }

    /**
     * 获取请求参数（排除敏感字段）
     */
    private String getRequestParams(ProceedingJoinPoint point, OperationLog annotation) {
        try {
            Object[] args = point.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }

            MethodSignature signature = (MethodSignature) point.getSignature();
            String[] paramNames = signature.getParameterNames();
            Map<String, Object> paramMap = new java.util.HashMap<>();
            for (int i = 0; i < args.length; i++) {
                // 跳过 HttpServletRequest 和 HttpServletResponse
                if (args[i] instanceof HttpServletRequest
                        || args[i] instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                String name = paramNames != null && i < paramNames.length
                        ? paramNames[i] : "arg" + i;
                // 对敏感字段进行脱敏
                Object value = args[i];
                if (isSensitiveParam(name) && value != null) {
                    value = "***";
                }
                paramMap.put(name, value);
            }
            String json = objectMapper.writeValueAsString(paramMap);
            return maskSensitiveData(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lowerName = paramName.toLowerCase();
        for (String sensitive : SENSITIVE_PARAMS) {
            if (lowerName.contains(sensitive.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取 HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * 获取用户名（从 SaToken 会话中获取）
     */
    private String getUsername(Long userId) {
        try {
            Object username = StpUtil.getSession().get("username");
            return username != null ? username.toString() : "用户-" + userId;
        } catch (Exception e) {
            return "用户-" + userId;
        }
    }

    /**
     * 获取真实姓名
     */
    private String getRealName(Long userId) {
        try {
            Object realName = StpUtil.getSession().get("realName");
            return realName != null ? realName.toString() : "用户-" + userId;
        } catch (Exception e) {
            return "用户-" + userId;
        }
    }
}
