package com.yigongbao.framework.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void methodNotSupportedHandlerAcceptsRequestToIdentifyEndpoint() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest("POST", "/order/modify/190/apply");
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("POST", java.util.Set.of("GET"));

        assertThat(handler.handleHttpRequestMethodNotSupportedException(exception, request).getCode())
                .isEqualTo(405);
        assertThat(HttpStatus.METHOD_NOT_ALLOWED.value()).isEqualTo(405);
    }
}
