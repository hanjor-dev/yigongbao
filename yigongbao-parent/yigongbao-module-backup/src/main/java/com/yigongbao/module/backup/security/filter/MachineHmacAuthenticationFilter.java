package com.yigongbao.module.backup.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.backup.common.enums.BackupErrorCode;
import com.yigongbao.module.backup.common.response.InternalApiResponse;
import com.yigongbao.module.backup.config.BackupProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 内部机器接口 HMAC 过滤器入口。
 *
 * <p>Task 3 提供完整验签器前，本过滤器采用 fail-closed 策略拒绝全部内部接口，避免未认证写操作意外开放。</p>
 */
public class MachineHmacAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MachineHmacAuthenticationFilter.class);
    private static final String INTERNAL_PREFIX = "/internal/";

    private final BackupProperties properties;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    public MachineHmacAuthenticationFilter(
            BackupProperties properties,
            Environment environment,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isExplicitTestBypass()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestId = resolveRequestId(request);
        log.warn("备份内部接口验证器尚未启用，拒绝请求 method={}, path={}, requestId={}",
                request.getMethod(), request.getRequestURI(), requestId);
        response.setStatus(BackupErrorCode.VERIFICATION_REJECTED.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                InternalApiResponse.error(BackupErrorCode.VERIFICATION_REJECTED, requestId));
    }

    private boolean isExplicitTestBypass() {
        return properties.getSecurity().isAllowUnverifiedRequestsForTests()
                && environment.acceptsProfiles(Profiles.of("test"));
    }

    private String resolveRequestId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
