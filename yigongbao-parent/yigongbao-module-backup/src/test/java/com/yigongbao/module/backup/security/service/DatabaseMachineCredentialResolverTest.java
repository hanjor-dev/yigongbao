package com.yigongbao.module.backup.security.service;

import com.yigongbao.module.backup.security.entity.MachineIdentityEntity;
import com.yigongbao.module.backup.security.entity.MachineKeyEntity;
import com.yigongbao.module.backup.security.mapper.MachineIdentityMapper;
import com.yigongbao.module.backup.security.mapper.MachineKeyMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseMachineCredentialResolverTest {

    @Test
    void rejectsKeyIdThatMatchesMoreThanOneMachine() {
        MachineKeyMapper keyMapper = mock(MachineKeyMapper.class);
        MachineIdentityMapper identityMapper = mock(MachineIdentityMapper.class);
        when(keyMapper.selectByKeyId("shared-key")).thenReturn(List.of(
                key("machine-01", "shared-key"), key("machine-02", "shared-key")));

        DatabaseMachineCredentialResolver resolver = resolver(keyMapper, identityMapper);

        assertThat(resolver.resolve("shared-key")).isNull();
    }

    @Test
    void resolvesGloballyUniqueKeyIdAndItsMachine() {
        MachineKeyMapper keyMapper = mock(MachineKeyMapper.class);
        MachineIdentityMapper identityMapper = mock(MachineIdentityMapper.class);
        when(keyMapper.selectByKeyId("unique-key")).thenReturn(List.of(key("machine-01", "unique-key")));
        MachineIdentityEntity machine = new MachineIdentityEntity();
        machine.setMachineId("machine-01");
        machine.setMachineStatus("ACTIVE");
        machine.setAllowedScopes("MANIFEST");
        when(identityMapper.selectByMachineId("machine-01")).thenReturn(machine);

        DatabaseMachineCredentialResolver resolver = resolver(keyMapper, identityMapper);

        assertThat(resolver.resolve("unique-key")).isNotNull()
                .extracting(credential -> credential.machineId(), credential -> credential.keyId())
                .containsExactly("machine-01", "unique-key");
    }

    private DatabaseMachineCredentialResolver resolver(
            MachineKeyMapper keyMapper,
            MachineIdentityMapper identityMapper
    ) {
        ObjectProvider<MachineKeyMapper> keyProvider = mock(ObjectProvider.class);
        ObjectProvider<MachineIdentityMapper> identityProvider = mock(ObjectProvider.class);
        when(keyProvider.getIfAvailable()).thenReturn(keyMapper);
        when(identityProvider.getIfAvailable()).thenReturn(identityMapper);
        return new DatabaseMachineCredentialResolver(keyProvider, identityProvider);
    }

    private MachineKeyEntity key(String machineId, String keyId) {
        MachineKeyEntity key = new MachineKeyEntity();
        key.setMachineId(machineId);
        key.setKeyId(keyId);
        key.setSecretReference("secret/" + keyId);
        key.setKeyStatus("ACTIVE");
        key.setValidFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        key.setAllowedScopes("MANIFEST");
        return key;
    }
}
