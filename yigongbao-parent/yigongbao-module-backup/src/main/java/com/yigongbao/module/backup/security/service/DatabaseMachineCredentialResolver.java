package com.yigongbao.module.backup.security.service;

import com.yigongbao.module.backup.security.entity.MachineIdentityEntity;
import com.yigongbao.module.backup.security.entity.MachineKeyEntity;
import com.yigongbao.module.backup.security.mapper.MachineIdentityMapper;
import com.yigongbao.module.backup.security.mapper.MachineKeyMapper;
import com.yigongbao.module.backup.security.model.MachineCredential;
import com.yigongbao.module.backup.security.model.MachineKeyStatus;
import com.yigongbao.module.backup.security.model.MachineScope;
import com.yigongbao.module.backup.security.model.MachineStatus;
import org.springframework.beans.factory.ObjectProvider;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 从在线元数据读取机器和 key，数据库只保存 secret_ref。 */
public class DatabaseMachineCredentialResolver implements MachineCredentialResolver {

    private final ObjectProvider<MachineKeyMapper> keyMappers;
    private final ObjectProvider<MachineIdentityMapper> identityMappers;

    public DatabaseMachineCredentialResolver(
            ObjectProvider<MachineKeyMapper> keyMappers,
            ObjectProvider<MachineIdentityMapper> identityMappers
    ) {
        this.keyMappers = keyMappers;
        this.identityMappers = identityMappers;
    }

    @Override
    public MachineCredential resolve(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            return null;
        }
        MachineKeyMapper keyMapper = keyMappers.getIfAvailable();
        MachineIdentityMapper identityMapper = identityMappers.getIfAvailable();
        if (keyMapper == null || identityMapper == null) {
            return null;
        }
        List<MachineKeyEntity> keys = keyMapper.selectByKeyId(keyId);
        // key_id 必须在全局范围内唯一。即使数据表当前仅有 (machine_id,key_id)
        // 联合唯一索引，发现重复也必须 fail-closed，不能由 LIMIT 1 随机放行。
        if (keys == null || keys.size() != 1) {
            return null;
        }
        MachineKeyEntity key = keys.getFirst();
        if (key.getMachineId() == null || !keyId.equals(key.getKeyId())) {
            return null;
        }
        MachineIdentityEntity machine = identityMapper.selectByMachineId(key.getMachineId());
        if (machine == null) {
            return null;
        }
        try {
            return new MachineCredential(
                    machine.getMachineId(),
                    MachineStatus.valueOf(machine.getMachineStatus().toUpperCase(Locale.ROOT)),
                    parseScopes(machine.getAllowedScopes()),
                    key.getKeyId(),
                    key.getSecretReference(),
                    MachineKeyStatus.valueOf(key.getKeyStatus().toUpperCase(Locale.ROOT)),
                    key.getValidFrom() == null ? null : key.getValidFrom().toInstant(ZoneOffset.UTC),
                    key.getValidUntil() == null ? null : key.getValidUntil().toInstant(ZoneOffset.UTC),
                    key.getRevokedAt() == null ? null : key.getRevokedAt().toInstant(ZoneOffset.UTC),
                    parseScopes(key.getAllowedScopes()));
        } catch (RuntimeException invalidMetadata) {
            return null;
        }
    }

    private Set<MachineScope> parseScopes(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        EnumSet<MachineScope> scopes = EnumSet.noneOf(MachineScope.class);
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(item -> MachineScope.valueOf(item.toUpperCase(Locale.ROOT)))
                .forEach(scopes::add);
        return scopes;
    }
}
