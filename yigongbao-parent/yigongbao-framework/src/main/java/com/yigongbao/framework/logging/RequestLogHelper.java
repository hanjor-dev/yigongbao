package com.yigongbao.framework.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 统一收集异常日志所需的请求上下文，并对敏感数据进行脱敏。
 */
@Component
public class RequestLogHelper {

    private static final int MAX_BODY_LENGTH = 8 * 1024;
    private static final Pattern SENSITIVE_QUERY_PATTERN = Pattern.compile(
            "(?i)(^|&)(password|passwd|secret|token|authorization|credential|key)=([^&]*)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> SENSITIVE_NAMES = List.of(
            "password", "passwd", "secret", "token", "authorization", "cookie",
            "credential", "privatekey", "accesskey", "证书密码"
    );

    public String build(HttpServletRequest request) {
        if (request == null) {
            return "{\"request\":\"request-context-unavailable\"}";
        }
        Map<String, Object> context = new LinkedHashMap<>();
        try {
            context.put("traceId", firstNonBlank(MDC.get("traceId"), request.getHeader("X-Trace-Id")));
            context.put("method", request.getMethod());
            context.put("uri", request.getRequestURI());
            context.put("query", maskQuery(request.getQueryString()));
            context.put("clientIp", getClientIp(request));
            context.put("userAgent", request.getHeader("User-Agent"));
            context.put("parameters", collectParameters(request));

            Object body = readBody(request);
            if (body != null) {
                context.put("body", body);
            }
            return OBJECT_MAPPER.writeValueAsString(context);
        } catch (Exception ignored) {
            return "{\"request\":\"request-context-unavailable\"}";
        }
    }

    private Map<String, Object> collectParameters(HttpServletRequest request) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        try {
            request.getParameterMap().forEach((name, values) -> {
                Object value = values == null ? null : values.length == 1 ? values[0] : Arrays.asList(values);
                parameters.put(name, isSensitive(name) ? "***" : value);
            });
        } catch (Exception ignored) {
            parameters.put("_unavailable", "[parameter-read-failed]");
        }
        return parameters;
    }

    private String maskQuery(String query) {
        if (query == null) {
            return null;
        }
        return SENSITIVE_QUERY_PATTERN.matcher(query).replaceAll("$1$2=***");
    }

    private Object readBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0 || isMultipart(request)) {
            return null;
        }

        String body = new String(content, StandardCharsets.UTF_8);
        if (request.getContentType() != null && request.getContentType().toLowerCase(Locale.ROOT).contains("json")) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(body);
                JsonNode masked = mask(node);
                String maskedJson = OBJECT_MAPPER.writeValueAsString(masked);
                return maskedJson.length() <= MAX_BODY_LENGTH ? masked : truncate(maskedJson);
            } catch (Exception ignored) {
                // JSON 解析失败时不记录原文，避免格式错误的请求绕过敏感字段脱敏。
                return "[unparseable json, length=" + content.length + "]";
            }
        }
        return "[body-omitted, contentType=" + request.getContentType()
                + ", length=" + content.length + "]";
    }

    private JsonNode mask(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.fieldNames().forEachRemaining(name -> {
                if (isSensitive(name)) {
                    object.put(name, "***");
                } else {
                    mask(object.get(name));
                }
            });
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            array.forEach(this::mask);
        }
        return node;
    }

    private boolean isSensitive(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return SENSITIVE_NAMES.stream().anyMatch(normalized::contains);
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private String truncate(String value) {
        if (value.length() <= MAX_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
