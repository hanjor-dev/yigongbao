package com.yigongbao.module.backup.security.verifier;

import com.yigongbao.module.backup.security.model.MachineScope;

/** 已通过验证并可传递给业务层的机器主体。 */
public record VerifiedMachine(String machineId, String keyId, MachineScope scope) {
}
