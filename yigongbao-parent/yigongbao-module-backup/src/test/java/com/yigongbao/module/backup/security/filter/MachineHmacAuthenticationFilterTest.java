package com.yigongbao.module.backup.security.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.model.MachineScope;
import com.yigongbao.module.backup.security.verifier.MachineRequestVerifier;
import com.yigongbao.module.backup.security.verifier.VerifiedMachine;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Test
    void verifiedRequestBodyIsCachedForVerifierAndReplayedToController() throws Exception {
        byte[] payload = "{\"marker\":\"cached-once\"}".getBytes(StandardCharsets.UTF_8);
        AtomicReference<byte[]> verifiedBody = new AtomicReference<>();
        MachineRequestVerifier verifier = (request, body) -> {
            verifiedBody.set(body);
            return new VerifiedMachine("machine-01", "key-v1", MachineScope.MANIFEST);
        };
        MachineHmacAuthenticationFilter filter = filter(
                new BackupProperties(), new MockEnvironment(), verifier);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/internal/backup/v1/candidates/reconcile");
        request.setContent(payload);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<byte[]> controllerBody = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                controllerBody.set(((HttpServletRequest) servletRequest).getInputStream().readAllBytes()));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(verifiedBody.get()).isEqualTo(payload);
        assertThat(controllerBody.get()).isEqualTo(payload);
    }

    @Test
    void rejectsRequestBodyBeforeAllocatingBeyondConfiguredLimit() throws ServletException, IOException {
        BackupProperties properties = new BackupProperties();
        properties.getSecurity().setMaxBodyBytes(4);
        AtomicBoolean verifierCalled = new AtomicBoolean();
        MachineRequestVerifier verifier = (request, body) -> {
            verifierCalled.set(true);
            return new VerifiedMachine("machine-01", "key-v1", MachineScope.MANIFEST);
        };
        MachineHmacAuthenticationFilter filter = filter(
                properties, new MockEnvironment(), verifier);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/internal/backup/v1/candidates/reconcile");
        request.setContent("12345".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(verifierCalled).isFalse();
    }

    private MachineHmacAuthenticationFilter filter(BackupProperties properties, MockEnvironment environment) {
        return new MachineHmacAuthenticationFilter(properties, environment, objectMapper);
    }

    private MachineHmacAuthenticationFilter filter(
            BackupProperties properties,
            MockEnvironment environment,
            MachineRequestVerifier verifier
    ) {
        return new MachineHmacAuthenticationFilter(properties, environment, objectMapper, verifier);
    }
}
