package com.yigongbao.module.backup.receipt;

import com.yigongbao.module.backup.receipt.entity.OrderBackupReceiptEntity;
import com.yigongbao.module.backup.receipt.model.ReceiptCommand;

public interface ReceiptService {
    OrderBackupReceiptEntity put(ReceiptCommand command);
    void changeStatus(Long receiptId, int expectedVersion, String status);
}
