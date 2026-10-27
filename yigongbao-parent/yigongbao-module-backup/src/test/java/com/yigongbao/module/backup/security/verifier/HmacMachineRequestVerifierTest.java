package com.yigongbao.module.backup.security.verifier;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.crypto.CanonicalRequest;
import com.yigongbao.module.backup.security.model.MachineCredential;
import com.yigongbao.module.backup.security.model.MachineKeyStatus;
import com.yigongbao.module.backup.security.model.MachineScope;
import com.yigongbao.module.backup.security.model.MachineStatus;
import com.yigongbao.module.backup.security.service.MachineCredentialResolver;
import com.yigongbao.module.backup.security.service.MachineSecretProvider;
import com.yigongbao.module.backup.security.service.NonceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacMachineRequestVerifierTest {

    private static final Instant NOW = Instant.parse("2026-10-26T06:00:00Z");
    private static final byte[] SECRET = "golden-secret".getBytes(StandardCharsets.UTF_8);
    private static final byte[] BODY = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);

    private final Map<String, MachineCredential> credentials = new ConcurrentHashMap<>();
    private final Map<String, byte[]> secrets = new ConcurrentHashMap<>();
    private final InMemoryNonceRegistry nonces = new InMemoryNonceRegistry();
    private BackupProperties properties;
    private HmacMachineRequestVerifier verifier;

    @BeforeEach
    void setUp() {
        properties = new BackupProperties();
        properties.getSecurity().setClockSkewSeconds(300);
        secrets.put("secret/golden", SECRET);
        credentials.put("key-v1", credential(
                "machine-01", "key-v1", "secret/golden", MachineKeyStatus.ACTIVE,
                NOW.minusSeconds(3600), NOW.plusSeconds(3600), null,
                EnumSet.allOf(MachineScope.class)));
        verifier = verifier(credentials::get, ref -> Optional.ofNullable(secrets.get(ref)), nonces);
    }

    @Test
    void canonicalSignatureMatchesGoldenVector() {
        MockHttpServletRequest request = signedRequest(
                "POST", "/internal/backup/v1/orders/42/manifest/7/confirm",
                "z=last&a=%E4%B8%AD", "key-v1", "nonce-001", "MANIFEST", "idem-001", BODY);

        assertThat(CanonicalRequest.create(request, BODY)).isEqualTo("""
                POST
                /internal/backup/v1/orders/42/manifest/7/confirm
                a=%E4%B8%AD&z=last
                4062edaf750fb8074e7e83e0c9028c94e32468a8b6f1614774328ef045150f93
                key-v1
                1792994400
                nonce-001
                MANIFEST
                idem-001""");
        assertThat(request.getHeader("X-Signature"))
                .isEqualTo("dc28c216fe38fb45471d14caa63781809596dee89f041cf8faee8ab2472b196d");

        VerifiedMachine result = verifier.verify(request, BODY);

        assertThat(result.machineId()).isEqualTo("machine-01");
        assertThat(result.keyId()).isEqualTo("key-v1");
        assertThat(result.scope()).isEqualTo(MachineScope.MANIFEST);
    }

    @Test
    void replayedNonceIsRejectedAfterAtomicFirstRegistration() {
        MockHttpServletRequest first = signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-v1", "same-nonce", "MANIFEST", "idem-a", BODY);
        MockHttpServletRequest replay = signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-v1", "same-nonce", "MANIFEST", "idem-b", BODY);

        assertThat(verifier.verify(first, BODY)).isNotNull();
        assertRejected(() -> verifier.verify(replay, BODY));
        assertThat(nonces.size()).isEqualTo(1);
    }

    @Test
    void expiredAndFutureTimestampsAreRejected() {
        MockHttpServletRequest expired = signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-v1", "expired", "MANIFEST", "idem-expired", BODY,
                NOW.minusSeconds(301));
        MockHttpServletRequest future = signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-v1", "future", "MANIFEST", "idem-future", BODY,
                NOW.plusSeconds(301));

        assertRejected(() -> verifier.verify(expired, BODY));
        assertRejected(() -> verifier.verify(future, BODY));
        assertThat(nonces.size()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-number", "92233720368547758070"})
    void malformedTimestampIsRejected(String timestamp) {
        MockHttpServletRequest request = baseRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-v1", "bad-time", "MANIFEST", "idem-time", timestamp);
        request.addHeader("X-Signature", "00".repeat(32));

        assertRejected(() -> verifier.verify(request, BODY));
        assertThat(nonces.size()).isZero();
    }

    @Test
    void wrongAndMissingScopeAreRejected() {
        MockHttpServletRequest wrong = signedRequest(
                "POST", "/internal/restore/v1/orders/42/prepare", null,
                "key-v1", "wrong-scope", "MANIFEST", "idem-wrong", BODY);
        MockHttpServletRequest missing = baseRequest(
                "POST", "/internal/restore/v1/orders/42/prepare", null,
                "key-v1", "missing-scope", null, "idem-missing", Long.toString(NOW.getEpochSecond()));
        missing.addHeader("X-Signature", "00".repeat(32));

        assertRejected(() -> verifier.verify(wrong, BODY));
        assertRejected(() -> verifier.verify(missing, BODY));
    }

    @Test
    void cleanupPermitEndpointsRequireDedicatedScope() {
        MockHttpServletRequest cleanup = signedRequest(
                "POST", "/internal/cleanup/v1/tasks/5/permits", null,
                "key-v1", "cleanup-scope", "CLEANUP", "idem-cleanup", BODY);
        MockHttpServletRequest permit = signedRequest(
                "POST", "/internal/cleanup/v1/tasks/5/permits", null,
                "key-v1", "permit-scope", "CLEANUP_PERMIT", "idem-permit", BODY);

        assertRejected(() -> verifier.verify(cleanup, BODY));
        assertThat(verifier.verify(permit, BODY).scope()).isEqualTo(MachineScope.CLEANUP_PERMIT);
    }

    @Test
    void revokedOldKeyFailsWhileOverlappingRetiringAndActiveKeysSucceed() {
        credentials.put("key-old", credential(
                "machine-01", "key-old", "secret/golden", MachineKeyStatus.RETIRING,
                NOW.minusSeconds(7200), NOW.plusSeconds(600), null,
                EnumSet.of(MachineScope.MANIFEST)));
        credentials.put("key-new", credential(
                "machine-01", "key-new", "secret/golden", MachineKeyStatus.ACTIVE,
                NOW.minusSeconds(60), NOW.plusSeconds(7200), null,
                EnumSet.of(MachineScope.MANIFEST)));

        assertThat(verifier.verify(signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-old", "old-overlap", "MANIFEST", "idem-old", BODY), BODY).keyId())
                .isEqualTo("key-old");
        assertThat(verifier.verify(signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-new", "new-overlap", "MANIFEST", "idem-new", BODY), BODY).keyId())
                .isEqualTo("key-new");

        credentials.put("key-old", credential(
                "machine-01", "key-old", "secret/golden", MachineKeyStatus.REVOKED,
                NOW.minusSeconds(7200), NOW.plusSeconds(600), NOW,
                EnumSet.of(MachineScope.MANIFEST)));
        MockHttpServletRequest revoked = signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-old", "old-revoked", "MANIFEST", "idem-revoked", BODY);

        assertRejected(() -> verifier.verify(revoked, BODY));
    }

    @Test
    void unknownExpiredAndSuspendedCredentialsAreRejected() {
        credentials.put("expired-key", credential(
                "machine-01", "expired-key", "secret/golden", MachineKeyStatus.ACTIVE,
                NOW.minusSeconds(7200), NOW.minusSeconds(1), null,
                EnumSet.of(MachineScope.MANIFEST)));
        MachineCredential suspended = new MachineCredential(
                "machine-02", MachineStatus.SUSPENDED, EnumSet.of(MachineScope.MANIFEST),
                "suspended-key", "secret/golden", MachineKeyStatus.ACTIVE,
                NOW.minusSeconds(60), NOW.plusSeconds(60), null,
                EnumSet.of(MachineScope.MANIFEST));
        credentials.put("suspended-key", suspended);

        assertRejected(() -> verifier.verify(signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "missing-key", "unknown", "MANIFEST", "idem-u", BODY), BODY));
        assertRejected(() -> verifier.verify(signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "expired-key", "expired-key-nonce", "MANIFEST", "idem-e", BODY), BODY));
        assertRejected(() -> verifier.verify(signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "suspended-key", "suspended", "MANIFEST", "idem-s", BODY), BODY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a=1&a=2", "a=1&%61=2", "q=%", "q=%GG", "q=%C3%28", "missingEquals"})
    void duplicateOrInvalidQueryEncodingIsRejected(String rawQuery) {
        MockHttpServletRequest request = baseRequest(
                "GET", "/internal/backup/v1/candidates/search", rawQuery,
                "key-v1", "query-" + Integer.toHexString(rawQuery.hashCode()),
                "MANIFEST", null, Long.toString(NOW.getEpochSecond()));
        request.addHeader("X-Signature", "00".repeat(32));

        assertRejected(() -> verifier.verify(request, new byte[0]));
        assertThat(nonces.size()).isZero();
    }

    @Test
    void writeRequestsRequireIdempotencyKeyAndAllAuthenticationHeaders() {
        MockHttpServletRequest noIdempotency = baseRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", null,
                "key-v1", "missing-idempotency", "MANIFEST", null,
                Long.toString(NOW.getEpochSecond()));
        noIdempotency.addHeader("X-Signature", "00".repeat(32));
        MockHttpServletRequest noKeyId = new MockHttpServletRequest(
                "POST", "/internal/backup/v1/candidates/reconcile");

        assertRejected(() -> verifier.verify(noIdempotency, BODY));
        assertRejected(() -> verifier.verify(noKeyId, BODY));
    }

    @Test
    void securityAuditLogContainsOnlySafeMetadata() {
        Logger logger = (Logger) LoggerFactory.getLogger(HmacMachineRequestVerifier.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        byte[] sensitiveBody = "{\"password\":\"body-secret-value\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = signedRequest(
                "POST", "/internal/backup/v1/candidates/reconcile", "private=full-url-secret",
                "key-v1", "audit-nonce", "MANIFEST", "idem-audit", sensitiveBody);
        String plaintextSignature = request.getHeader("X-Signature");

        try {
            verifier.verify(request, sensitiveBody);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("keyId=key-v1", "machineId=machine-01", "scope=MANIFEST",
                        "result=ACCEPTED", "requestId=request-safe-id")
                .doesNotContain("golden-secret", "body-secret-value", plaintextSignature,
                        "private=full-url-secret", "/internal/backup");
    }

    private HmacMachineRequestVerifier verifier(
            MachineCredentialResolver resolver,
            MachineSecretProvider secretProvider,
            NonceRegistry nonceRegistry
    ) {
        return new HmacMachineRequestVerifier(
                resolver, secretProvider, nonceRegistry, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private MachineCredential credential(
            String machineId,
            String keyId,
            String secretRef,
            MachineKeyStatus keyStatus,
            Instant validFrom,
            Instant validUntil,
            Instant revokedAt,
            EnumSet<MachineScope> scopes
    ) {
        return new MachineCredential(
                machineId, MachineStatus.ACTIVE, EnumSet.allOf(MachineScope.class),
                keyId, secretRef, keyStatus, validFrom, validUntil, revokedAt, scopes);
    }

    private MockHttpServletRequest signedRequest(
            String method,
            String path,
            String rawQuery,
            String keyId,
            String nonce,
            String scope,
            String idempotencyKey,
            byte[] body
    ) {
        return signedRequest(method, path, rawQuery, keyId, nonce, scope, idempotencyKey, body, NOW);
    }

    private MockHttpServletRequest signedRequest(
            String method,
            String path,
            String rawQuery,
            String keyId,
            String nonce,
            String scope,
            String idempotencyKey,
            byte[] body,
            Instant timestamp
    ) {
        MockHttpServletRequest request = baseRequest(
                method, path, rawQuery, keyId, nonce, scope, idempotencyKey,
                Long.toString(timestamp.getEpochSecond()));
        request.addHeader("X-Signature", hmacHex(SECRET, CanonicalRequest.create(request, body)));
        return request;
    }

    private MockHttpServletRequest baseRequest(
            String method,
            String path,
            String rawQuery,
            String keyId,
            String nonce,
            String scope,
            String idempotencyKey,
            String timestamp
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setQueryString(rawQuery);
        addHeader(request, "X-Key-Id", keyId);
        addHeader(request, "X-Nonce", nonce);
        addHeader(request, "X-Timestamp", timestamp);
        addHeader(request, "X-Scope", scope);
        addHeader(request, "Idempotency-Key", idempotencyKey);
        request.addHeader("X-Request-Id", "request-safe-id");
        return request;
    }

    private void addHeader(MockHttpServletRequest request, String name, String value) {
        if (value != null) {
            request.addHeader(name, value);
        }
    }

    private String hmacHex(byte[] secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void assertRejected(Runnable invocation) {
        assertThatThrownBy(invocation::run).isInstanceOf(MachineVerificationException.class);
    }

    private static final class InMemoryNonceRegistry implements NonceRegistry {

        private final Map<String, Boolean> registrations = new ConcurrentHashMap<>();

        @Override
        public boolean register(
                String machineId,
                String keyId,
                String nonceHash,
                Instant requestTime,
                Instant expiresAt
        ) {
            return registrations.putIfAbsent(machineId + '\n' + keyId + '\n' + nonceHash, Boolean.TRUE) == null;
        }

        int size() {
            return registrations.size();
        }
    }
}
