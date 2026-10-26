package com.yigongbao.module.backup.security.service;

import java.time.Instant;

/** 原子登记已验签 nonce 的防重放边界。 */
@FunctionalInterface
public interface NonceRegistry {

    /**
     * @return 首次登记返回 true；唯一约束冲突返回 false
     */
    boolean register(
            String machineId,
            String keyId,
            String nonceHash,
            Instant requestTime,
            Instant expiresAt
    );
}
