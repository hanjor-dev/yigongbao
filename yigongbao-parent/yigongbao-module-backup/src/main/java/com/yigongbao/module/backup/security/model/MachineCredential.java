package com.yigongbao.module.backup.security.model;

import java.time.Instant;
import java.util.Set;

/** 从线上元数据组装的可验证机器凭据，不包含密钥正文。 */
public record MachineCredential(
        String machineId,
        MachineStatus machineStatus,
        Set<MachineScope> machineScopes,
        String keyId,
        String secretReference,
        MachineKeyStatus keyStatus,
        Instant validFrom,
        Instant validUntil,
        Instant revokedAt,
        Set<MachineScope> keyScopes
) {
    public MachineCredential {
        machineScopes = Set.copyOf(machineScopes);
        keyScopes = Set.copyOf(keyScopes);
    }
}
