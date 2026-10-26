package com.yigongbao.module.backup.receipt;

import com.yigongbao.module.backup.receipt.entity.OrderBackupReceiptEntity;
import com.yigongbao.module.backup.receipt.mapper.OrderBackupReceiptMapper;
import com.yigongbao.module.backup.receipt.model.ReceiptCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReceiptServiceImplTest {
    private final OrderBackupReceiptMapper mapper = mock(OrderBackupReceiptMapper.class);
    private final ReceiptServiceImpl service = new ReceiptServiceImpl(mapper);
    private final ReceiptCommand command = new ReceiptCommand(1L, "rev-1", "a".repeat(64),
            "b".repeat(64), 2, 10L, "idem-1", 1, LocalDateTime.now());

    @Test
    void sameIdempotencyAndDigestReturnsExistingReceipt() {
        OrderBackupReceiptEntity existing = new OrderBackupReceiptEntity();
        existing.setManifestSha256(command.manifestSha256());
        existing.setPackageSha256(command.packageSha256());
        when(mapper.selectByIdempotencyKey("idem-1")).thenReturn(existing);

        assertThat(service.put(command)).isSameAs(existing);
        verify(mapper, never()).insert(any(OrderBackupReceiptEntity.class));
    }

    @Test
    void idempotencyDigestConflictIsRejected() {
        OrderBackupReceiptEntity existing = new OrderBackupReceiptEntity();
        existing.setManifestSha256("c".repeat(64));
        existing.setPackageSha256(command.packageSha256());
        when(mapper.selectByIdempotencyKey("idem-1")).thenReturn(existing);

        assertThatThrownBy(() -> service.put(command))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void stateCasConflictIsRejected() {
        when(mapper.updateStatusCas(3L, 1, "SUSPENDED")).thenReturn(0);

        assertThatThrownBy(() -> service.changeStatus(3L, 1, "SUSPENDED"))
                .isInstanceOf(IllegalStateException.class);
    }
}
