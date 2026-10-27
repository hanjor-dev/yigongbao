package com.yigongbao.module.backup.security.service;

import com.yigongbao.module.backup.security.model.MachineCredential;

/** 按 key ID 解析机器与密钥元数据。 */
@FunctionalInterface
public interface MachineCredentialResolver {

    /** 未找到或 key ID 不唯一时返回 {@code null}。 */
    MachineCredential resolve(String keyId);
}
