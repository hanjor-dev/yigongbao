package com.yigongbao.framework.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogHelperTest {

    @Test
    void collectsRequestMetadataParametersAndMasksSensitiveBodyFields() throws Exception {
        MockHttpServletRequest original = new MockHttpServletRequest("POST", "/order/modify/190/apply");
        original.setQueryString("stage=2&token=secret-token");
        original.addParameter("pageSize", "10");
        original.setRemoteAddr("10.0.0.8");
        original.addHeader("User-Agent", "test-client");
        original.setContentType("application/json");
        original.setCharacterEncoding(StandardCharsets.UTF_8.name());
        original.setContent("{\"username\":\"admin\",\"password\":\"secret\",\"nested\":{\"token\":\"abc\"}}".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(original);
        request.getInputStream().readAllBytes();

        String actual = new RequestLogHelper().build(request);

        assertThat(actual).contains("\"method\":\"POST\"")
                .contains("\"uri\":\"/order/modify/190/apply\"")
                .contains("\"query\":\"stage=2&token=***\"")
                .contains("\"pageSize\":\"10\"")
                .contains("\"password\":\"***\"")
                .contains("\"token\":\"***\"")
                .doesNotContain("secret")
                .doesNotContain("abc");
    }

    @Test
    void returnsSafeFallbackWhenRequestMetadataCannotBeRead() {
        String actual = new RequestLogHelper().build(null);

        assertThat(actual).contains("request-context-unavailable");
    }

    @Test
    void doesNotWriteUnparseableJsonBodyToLogs() throws Exception {
        MockHttpServletRequest original = new MockHttpServletRequest("POST", "/login");
        original.setContentType("application/json");
        original.setContent("{\"password\":\"secret\"".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(original);
        request.getInputStream().readAllBytes();

        String actual = new RequestLogHelper().build(request);

        assertThat(actual).contains("unparseable json").doesNotContain("secret");
    }

    @Test
    void doesNotWriteNonJsonBodyContentsToLogs() throws Exception {
        MockHttpServletRequest original = new MockHttpServletRequest("POST", "/callback");
        original.setContentType("text/plain");
        original.setContent("password=secret".getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(original);
        request.getInputStream().readAllBytes();

        String actual = new RequestLogHelper().build(request);

        assertThat(actual).contains("body-omitted").doesNotContain("secret");
    }
}
