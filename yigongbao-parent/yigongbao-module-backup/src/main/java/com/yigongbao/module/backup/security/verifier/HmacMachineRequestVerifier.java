package com.yigongbao.module.backup.security.verifier;

import com.yigongbao.module.backup.config.BackupProperties;
import com.yigongbao.module.backup.security.crypto.CanonicalRequest;
import com.yigongbao.module.backup.security.model.MachineCredential;
import com.yigongbao.module.backup.security.model.MachineKeyStatus;
import com.yigongbao.module.backup.security.model.MachineScope;
import com.yigongbao.module.backup.security.model.MachineStatus;
import com.yigongbao.module.backup.security.service.MachineCredentialResolver;
import com.yigongbao.module.backup.security.service.MachineSecretProvider;
import com.yigongbao.module.backup.security.service.NonceRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** HMAC-SHA256 机器请求验证器。 */
public class HmacMachineRequestVerifier implements MachineRequestVerifier {

    private static final Logger log = LoggerFactory.getLogger(HmacMachineRequestVerifier.class);
    private static final HexFormat HEX = HexFormat.of();
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final MachineCredentialResolver credentialResolver;
    private final MachineSecretProvider secretProvider;
    private final NonceRegistry nonceRegistry;
    private final BackupProperties properties;
    private final Clock clock;

    @Autowired
    public HmacMachineRequestVerifier(
            MachineCredentialResolver credentialResolver,
            MachineSecretProvider secretProvider,
            NonceRegistry nonceRegistry,
            BackupProperties properties
    ) {
        this(credentialResolver, secretProvider, nonceRegistry, properties, Clock.systemUTC());
    }

    public HmacMachineRequestVerifier(
            MachineCredentialResolver credentialResolver,
            MachineSecretProvider secretProvider,
            NonceRegistry nonceRegistry,
            BackupProperties properties,
            Clock clock
    ) {
        this.credentialResolver = credentialResolver;
        this.secretProvider = secretProvider;
        this.nonceRegistry = nonceRegistry;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public VerifiedMachine verify(HttpServletRequest request, byte[] cachedBody) {
        if (request == null || cachedBody == null) {
            throw rejected();
        }
        String keyId = auditValue(request.getHeader("X-Key-Id"));
        String machineId = "-";
        String scopeValue = auditValue(request.getHeader("X-Scope"));
        String requestId = resolveRequestId(request);
        try {
            String nonce = requiredHeader(request, "X-Nonce", 256);
            String timestampValue = requiredHeader(request, "X-Timestamp", 32);
            String signatureValue = requiredHeader(request, "X-Signature", 64);
            keyId = requiredHeader(request, "X-Key-Id", 128);
            scopeValue = requiredHeader(request, "X-Scope", 32);
            if (isWrite(request)) {
                requiredHeader(request, "Idempotency-Key", 256);
            }

            MachineScope suppliedScope = parseScope(scopeValue);
            if (requiredScope(request) != suppliedScope) {
                throw rejected();
            }
            Instant requestTime = parseTimestamp(timestampValue);
            Instant now = clock.instant();
            long skewSeconds = properties.getSecurity().getClockSkewSeconds();
            if (skewSeconds < 0 || skewSeconds > 86_400
                    || requestTime.isBefore(now.minusSeconds(skewSeconds))
                    || requestTime.isAfter(now.plusSeconds(skewSeconds))) {
                throw rejected();
            }

            MachineCredential credential = credentialResolver.resolve(keyId);
            if (credential == null) {
                throw rejected();
            }
            if (!keyId.equals(credential.keyId())) {
                throw rejected();
            }
            machineId = auditValue(credential.machineId());
            validateCredential(credential, suppliedScope, now);

            byte[] secret = secretProvider.resolveSecret(credential.secretReference())
                    .map(value -> value.clone())
                    .orElseThrow(MachineVerificationException::new);
            try {
                byte[] suppliedSignature = parseSignature(signatureValue);
                byte[] expectedSignature = sign(secret, CanonicalRequest.create(request, cachedBody));
                if (!MessageDigest.isEqual(expectedSignature, suppliedSignature)) {
                    throw rejected();
                }
            } finally {
                Arrays.fill(secret, (byte) 0);
            }

            String nonceHash = CanonicalRequest.sha256Hex(nonce.getBytes(StandardCharsets.UTF_8));
            if (!nonceRegistry.register(
                    credential.machineId(), credential.keyId(), nonceHash,
                    requestTime, now.plusSeconds(skewSeconds))) {
                throw rejected();
            }

            auditAccepted(keyId, machineId, suppliedScope.name(), requestId);
            return new VerifiedMachine(credential.machineId(), credential.keyId(), suppliedScope);
        } catch (MachineVerificationException exception) {
            auditRejected(keyId, machineId, scopeValue, requestId);
            throw exception;
        } catch (IllegalArgumentException | DateTimeException exception) {
            auditRejected(keyId, machineId, scopeValue, requestId);
            throw rejected();
        } catch (RuntimeException exception) {
            auditRejected(keyId, machineId, scopeValue, requestId);
            throw rejected();
        }
    }

    private void validateCredential(MachineCredential credential, MachineScope scope, Instant now) {
        if (credential.machineStatus() != MachineStatus.ACTIVE
                || (credential.keyStatus() != MachineKeyStatus.ACTIVE
                    && credential.keyStatus() != MachineKeyStatus.RETIRING)
                || credential.revokedAt() != null
                || credential.validFrom() == null
                || now.isBefore(credential.validFrom())
                || (credential.validUntil() != null && now.isAfter(credential.validUntil()))
                || !credential.machineScopes().contains(scope)
                || !credential.keyScopes().contains(scope)) {
            throw rejected();
        }
    }

    private MachineScope parseScope(String value) {
        try {
            return MachineScope.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw rejected();
        }
    }

    private MachineScope requiredScope(HttpServletRequest request) {
        String path = contextRelativePath(request);
        if (path.startsWith("/internal/restore/")) {
            return MachineScope.RESTORE;
        }
        if (path.startsWith("/internal/cleanup/")) {
            if (path.contains("/permit-requests") || path.contains("/permits")) {
                return MachineScope.CLEANUP_PERMIT;
            }
            return MachineScope.CLEANUP;
        }
        if (path.startsWith("/internal/backup/v1/users")) {
            return MachineScope.ACCOUNT;
        }
        if (path.startsWith("/internal/backup/")) {
            return MachineScope.MANIFEST;
        }
        throw rejected();
    }

    private String contextRelativePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = Optional.ofNullable(request.getContextPath()).orElse("");
        if (!contextPath.isEmpty()) {
            if (!uri.startsWith(contextPath)) {
                throw rejected();
            }
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private Instant parseTimestamp(String value) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(value));
        } catch (NumberFormatException | DateTimeException exception) {
            throw rejected();
        }
    }

    private byte[] parseSignature(String value) {
        if (value.length() != 64) {
            throw rejected();
        }
        try {
            return HEX.parseHex(value);
        } catch (IllegalArgumentException exception) {
            throw rejected();
        }
    }

    private byte[] sign(byte[] secret, String canonicalRequest) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", exception);
        }
    }

    private boolean isWrite(HttpServletRequest request) {
        return WRITE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT));
    }

    private String requiredHeader(HttpServletRequest request, String name, int maxLength) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank() || value.length() > maxLength
                || value.chars().anyMatch(character -> character < 0x20 || character == 0x7f)) {
            throw rejected();
        }
        return value;
    }

    private MachineVerificationException rejected() {
        return new MachineVerificationException();
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object requestAttribute = request.getAttribute("backup.security.requestId");
        if (requestAttribute instanceof String value && !value.isBlank()) {
            return auditValue(value);
        }
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return auditValue(traceId);
        }
        return auditValue(request.getHeader("X-Request-Id"));
    }

    private String auditValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replaceAll("[^A-Za-z0-9._:-]", "_");
        return sanitized.substring(0, Math.min(128, sanitized.length()));
    }

    private void auditAccepted(String keyId, String machineId, String scope, String requestId) {
        log.info("machine_security_audit keyId={} machineId={} scope={} result={} requestId={}",
                auditValue(keyId), auditValue(machineId), auditValue(scope), "ACCEPTED", auditValue(requestId));
    }

    private void auditRejected(String keyId, String machineId, String scope, String requestId) {
        log.warn("machine_security_audit keyId={} machineId={} scope={} result={} requestId={}",
                auditValue(keyId), auditValue(machineId), auditValue(scope), "REJECTED", auditValue(requestId));
    }
}
