package com.yigongbao.module.backup.security.service;

import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 生产密钥解析器。
 *
 * <p>{@code backup_machine_key.secret_ref} 存储 Spring 配置键，实际密钥由环境变量或外部配置注入。</p>
 */
public class ConfiguredMachineSecretProvider implements MachineSecretProvider {

    private final Environment environment;

    public ConfiguredMachineSecretProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Optional<byte[]> resolveSecret(String secretReference) {
        if (secretReference == null || secretReference.isBlank()) {
            return Optional.empty();
        }
        String value = environment.getProperty(secretReference);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.getBytes(StandardCharsets.UTF_8));
    }
}
