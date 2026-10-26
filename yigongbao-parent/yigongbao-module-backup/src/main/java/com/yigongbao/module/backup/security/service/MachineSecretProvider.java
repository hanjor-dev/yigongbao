package com.yigongbao.module.backup.security.service;

import java.util.Optional;

/** 从外部配置引用解析 HMAC 密钥；数据库只保存引用。 */
@FunctionalInterface
public interface MachineSecretProvider {

    Optional<byte[]> resolveSecret(String secretReference);
}
