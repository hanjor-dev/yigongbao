package com.yigongbao.module.backup.receipt;

import com.yigongbao.module.backup.receipt.entity.OrderBackupReceiptEntity;
import com.yigongbao.module.backup.receipt.mapper.OrderBackupReceiptMapper;
import com.yigongbao.module.backup.receipt.model.ReceiptCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** 凭证版本、当前唯一性和状态 CAS 服务。 */
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {
    private final OrderBackupReceiptMapper mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderBackupReceiptEntity put(ReceiptCommand command) {
        validate(command);
        OrderBackupReceiptEntity existing = mapper.selectByIdempotencyKey(command.idempotencyKey());
        if (existing != null) {
            if (!Objects.equals(existing.getManifestSha256(), command.manifestSha256())
                    || !Objects.equals(existing.getPackageSha256(), command.packageSha256())) {
                throw new IllegalStateException("幂等键已用于不同凭证摘要");
            }
            return existing;
        }
        OrderBackupReceiptEntity previous = mapper.selectCurrent(command.orderId());
        OrderBackupReceiptEntity entity = new OrderBackupReceiptEntity();
        entity.setOrderId(command.orderId());
        entity.setManifestRevision(command.manifestRevision());
        entity.setManifestSha256(command.manifestSha256());
        entity.setPackageSha256(command.packageSha256());
        entity.setFileCount(command.fileCount());
        entity.setSourceTotalSize(command.sourceTotalSize());
        entity.setLocalIdempotencyKey(command.idempotencyKey());
        entity.setReceiptVersion(command.receiptVersion());
        entity.setPreviousReceiptId(previous == null ? null : previous.getId());
        entity.setIsCurrent(1);
        entity.setStateVersion(0);
        entity.setBackupCompletedAt(command.backupCompletedAt());
        entity.setReceiptStatus("ACTIVE");
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException duplicate) {
            OrderBackupReceiptEntity raced = mapper.selectByIdempotencyKey(command.idempotencyKey());
            if (raced != null && Objects.equals(raced.getManifestSha256(), command.manifestSha256())
                    && Objects.equals(raced.getPackageSha256(), command.packageSha256())) {
                return raced;
            }
            throw duplicate;
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long receiptId, int expectedVersion, String status) {
        if (receiptId == null || status == null || !status.matches("ACTIVE|SUSPENDED|INVALIDATED")) {
            throw new IllegalArgumentException("凭证状态参数无效");
        }
        if (mapper.updateStatusCas(receiptId, expectedVersion, status) != 1) {
            throw new IllegalStateException("凭证状态版本冲突");
        }
    }

    private void validate(ReceiptCommand command) {
        if (command == null || command.orderId() == null || command.receiptVersion() < 1
                || command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                || command.manifestSha256() == null || !command.manifestSha256().matches("[0-9a-f]{64}")
                || command.packageSha256() == null || !command.packageSha256().matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("凭证摘要参数无效");
        }
    }
}
