package com.yigongbao.framework.logging;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogFilterTest {

    @Test
    void generatesSafeTraceIdAndLimitsCachedRequestBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/upload");
        request.addHeader(RequestLogFilter.TRACE_ID_HEADER, "bad\r\ntrace");
        request.setContent("x".repeat(32 * 1024).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                                 jakarta.servlet.ServletResponse servletResponse) throws IOException {
                HttpServletRequest wrapped = (HttpServletRequest) servletRequest;
                wrapped.getInputStream().readAllBytes();
                assertThat(wrapped).isInstanceOf(ContentCachingRequestWrapper.class);
                assertThat(((ContentCachingRequestWrapper) wrapped).getContentAsByteArray()).hasSizeLessThanOrEqualTo(16 * 1024);
            }
        };

        new RequestLogFilter().doFilter(request, response, chain);

        assertThat(response.getHeader(RequestLogFilter.TRACE_ID_HEADER))
                .matches("[0-9a-f]{32}");
    }
}
