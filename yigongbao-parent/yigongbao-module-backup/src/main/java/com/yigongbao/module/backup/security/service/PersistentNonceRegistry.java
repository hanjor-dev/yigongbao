package com.yigongbao.module.backup.security.service;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.yigongbao.module.backup.security.entity.MachineNonceEntity;
import com.yigongbao.module.backup.security.mapper.MachineNonceMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;

/** 通过数据库唯一索引原子登记 nonce；持久化不可用时 fail closed。 */
public class PersistentNonceRegistry implements NonceRegistry {

    private final ObjectProvider<MachineNonceMapper> mappers;

    public PersistentNonceRegistry(ObjectProvider<MachineNonceMapper> mappers) {
        this.mappers = mappers;
    }

    @Override
    public boolean register(
            String machineId,
            String keyId,
            String nonceHash,
            Instant requestTime,
            Instant expiresAt
    ) {
        MachineNonceMapper mapper = mappers.getIfAvailable();
        if (mapper == null) {
            return false;
        }
        MachineNonceEntity entity = new MachineNonceEntity();
        entity.setMachineId(machineId);
        entity.setKeyId(keyId);
        entity.setNonceHash(nonceHash);
        entity.setRequestTimestamp(requestTime);
        entity.setExpiresAt(expiresAt);
        try {
            return mapper.insert(entity) == 1;
        } catch (DuplicateKeyException | MybatisPlusException duplicateOrUnavailable) {
            return false;
        }
    }
}
