package com.yigongbao.framework.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 为请求生成 TraceId，并缓存请求体供异常日志使用。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final int MAX_CACHED_REQUEST_BYTES = 16 * 1024;
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || !SAFE_TRACE_ID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        HttpServletRequest wrappedRequest = request instanceof ContentCachingRequestWrapper
                ? request : new ContentCachingRequestWrapper(request, MAX_CACHED_REQUEST_BYTES);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
