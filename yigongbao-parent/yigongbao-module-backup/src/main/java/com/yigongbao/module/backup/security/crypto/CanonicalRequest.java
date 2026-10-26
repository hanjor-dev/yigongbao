package com.yigongbao.module.backup.security.crypto;

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** HMAC 的唯一规范请求表示。 */
public final class CanonicalRequest {

    private static final HexFormat HEX = HexFormat.of();

    private CanonicalRequest() {
    }

    /**
     * 规范形式为：method、context-relative path、规范 query、body SHA-256、
     * key ID、timestamp、nonce、scope、idempotency key，各占一行。
     */
    public static String create(HttpServletRequest request, byte[] body) {
        return String.join("\n",
                request.getMethod().toUpperCase(Locale.ROOT),
                contextRelativePath(request),
                canonicalQuery(request.getQueryString()),
                sha256Hex(body),
                requiredHeader(request, "X-Key-Id"),
                requiredHeader(request, "X-Timestamp"),
                requiredHeader(request, "X-Nonce"),
                requiredHeader(request, "X-Scope"),
                headerOrEmpty(request, "Idempotency-Key"));
    }

    public static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<QueryPart> parts = new ArrayList<>();
        Set<String> decodedNames = new HashSet<>();
        for (String pair : rawQuery.split("&", -1)) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("invalid query");
            }
            String name = strictPercentDecode(pair.substring(0, separator));
            String value = strictPercentDecode(pair.substring(separator + 1));
            if (name.isEmpty() || !decodedNames.add(name)) {
                throw new IllegalArgumentException("ambiguous query");
            }
            parts.add(new QueryPart(percentEncode(name), percentEncode(value)));
        }
        parts.sort(Comparator.comparing(QueryPart::name));
        return parts.stream()
                .map(part -> part.name() + "=" + part.value())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    public static String sha256Hex(byte[] value) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String contextRelativePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return requestUri;
        }
        if (!requestUri.startsWith(contextPath)) {
            throw new IllegalArgumentException("invalid context path");
        }
        String path = requestUri.substring(contextPath.length());
        return path.isEmpty() ? "/" : path;
    }

    private static String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank() || containsControl(value)) {
            throw new IllegalArgumentException("missing canonical header");
        }
        return value;
    }

    private static String headerOrEmpty(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null) {
            return "";
        }
        if (containsControl(value)) {
            throw new IllegalArgumentException("invalid canonical header");
        }
        return value;
    }

    private static boolean containsControl(String value) {
        return value.chars().anyMatch(character -> character < 0x20 || character == 0x7f);
    }

    private static String strictPercentDecode(String raw) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(raw.length());
        for (int index = 0; index < raw.length();) {
            char character = raw.charAt(index);
            if (character == '%') {
                if (index + 2 >= raw.length()) {
                    throw new IllegalArgumentException("invalid percent encoding");
                }
                int high = Character.digit(raw.charAt(index + 1), 16);
                int low = Character.digit(raw.charAt(index + 2), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException("invalid percent encoding");
                }
                bytes.write((high << 4) + low);
                index += 3;
            } else {
                int codePoint = raw.codePointAt(index);
                byte[] encoded = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
                bytes.writeBytes(encoded);
                index += Character.charCount(codePoint);
            }
        }
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()));
            return decoded.toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("invalid utf-8 query", e);
        }
    }

    private static String percentEncode(String value) {
        StringBuilder result = new StringBuilder();
        for (byte current : value.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = current & 0xff;
            if ((unsigned >= 'a' && unsigned <= 'z')
                    || (unsigned >= 'A' && unsigned <= 'Z')
                    || (unsigned >= '0' && unsigned <= '9')
                    || unsigned == '-' || unsigned == '.' || unsigned == '_' || unsigned == '~') {
                result.append((char) unsigned);
            } else {
                result.append('%');
                result.append(Character.toUpperCase(Character.forDigit(unsigned >>> 4, 16)));
                result.append(Character.toUpperCase(Character.forDigit(unsigned & 0x0f, 16)));
            }
        }
        return result.toString();
    }

    private record QueryPart(String name, String value) {
    }
}
