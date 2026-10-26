package com.yigongbao.module.backup.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.backup.common.enums.BackupErrorCode;
import com.yigongbao.module.backup.common.response.InternalApiResponse;
import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.verifier.MachineRequestVerifier;
import com.yigongbao.module.backup.security.verifier.MachineVerificationException;
import com.yigongbao.module.backup.security.verifier.VerifiedMachine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 内部机器接口 HMAC 过滤器入口。
 *
 * <p>请求体只读取一次并以缓存包装器转发，使验签与业务控制器看到完全相同的字节。</p>
 */
public class MachineHmacAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PREFIX = "/internal/";

    private final BackupProperties properties;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final MachineRequestVerifier verifier;

    public MachineHmacAuthenticationFilter(
            BackupProperties properties,
            Environment environment,
            ObjectMapper objectMapper
    ) {
        this(properties, environment, objectMapper, (request, body) -> {
            throw new MachineVerificationException();
        });
    }

    public MachineHmacAuthenticationFilter(
            BackupProperties properties,
            Environment environment,
            ObjectMapper objectMapper,
            MachineRequestVerifier verifier
    ) {
        this.properties = properties;
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String contextRelativePath = contextPath.isEmpty()
                ? requestUri
                : requestUri.startsWith(contextPath)
                    ? requestUri.substring(contextPath.length())
                    : requestUri;
        return !contextRelativePath.startsWith(INTERNAL_PREFIX);
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
        CachedBodyRequest cachedRequest;
        try {
            byte[] body = request.getInputStream().readAllBytes();
            cachedRequest = new CachedBodyRequest(request, body);
            requestId = resolveRequestId(cachedRequest);
            VerifiedMachine machine = verifier.verify(cachedRequest, body);
            if (machine == null) {
                throw new MachineVerificationException();
            }
            cachedRequest.setAttribute("backup.security.machine", machine);
            cachedRequest.setAttribute("backup.security.requestId", requestId);
        } catch (IOException | RuntimeException exception) {
            reject(response, requestId);
            return;
        }
        filterChain.doFilter(cachedRequest, response);
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

    private void reject(HttpServletResponse response, String requestId) throws IOException {
        response.setStatus(BackupErrorCode.VERIFICATION_REJECTED.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                InternalApiResponse.error(BackupErrorCode.VERIFICATION_REJECTED, requestId));
    }

    /** Servlet request wrapper that returns a fresh stream over the cached bytes on every call. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncodingOrUtf8()));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        private java.nio.charset.Charset getCharacterEncodingOrUtf8() {
            try {
                String encoding = getCharacterEncoding();
                return encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding);
            } catch (Exception ignored) {
                return StandardCharsets.UTF_8;
            }
        }
    }
}
