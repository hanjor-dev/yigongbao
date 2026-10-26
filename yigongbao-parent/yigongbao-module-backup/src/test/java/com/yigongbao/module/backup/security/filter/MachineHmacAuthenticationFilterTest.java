package com.yigongbao.module.backup.security.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.backup.config.BackupProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MachineHmacAuthenticationFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsInternalWriteRouteUntilVerifierIsImplemented() throws ServletException, IOException {
        MachineHmacAuthenticationFilter filter = filter(new BackupProperties(), new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/internal/backup/v1/candidates/reconcile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("code").asText()).isEqualTo("VERIFICATION_REJECTED");
        assertThat(body.path("retryable").asBoolean()).isFalse();
        assertThat(body.path("requestId").asText()).isNotBlank();
    }

    @Test
    void rejectsInternalWriteRouteWhenApplicationHasContextPath() throws ServletException, IOException {
        MachineHmacAuthenticationFilter filter = filter(new BackupProperties(), new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/internal/backup/v1/candidates/reconcile");
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.path("code").asText()).isEqualTo("VERIFICATION_REJECTED");
    }

    @Test
    void doesNotInterceptNonInternalApi() throws ServletException, IOException {
        MachineHmacAuthenticationFilter filter = filter(new BackupProperties(), new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void explicitBypassOnlyWorksWithTestProfile() throws ServletException, IOException {
        BackupProperties properties = new BackupProperties();
        properties.getSecurity().setAllowUnverifiedRequestsForTests(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        MachineHmacAuthenticationFilter filter = filter(properties, environment);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/internal/restore/v1/sessions/1/commit");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void testBypassFlagCannotOpenRoutesOutsideTestProfile() throws ServletException, IOException {
        BackupProperties properties = new BackupProperties();
        properties.getSecurity().setAllowUnverifiedRequestsForTests(true);
        MachineHmacAuthenticationFilter filter = filter(properties, new MockEnvironment());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/internal/cleanup/v1/tasks");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private MachineHmacAuthenticationFilter filter(BackupProperties properties, MockEnvironment environment) {
        return new MachineHmacAuthenticationFilter(properties, environment, objectMapper);
    }
}
