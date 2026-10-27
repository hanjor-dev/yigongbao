package com.yigongbao.module.backup.receipt.model;

import java.time.LocalDateTime;

/** 本地上传的订单凭证摘要；幂等键相同但摘要不同必须拒绝。 */
public record ReceiptCommand(Long orderId, String manifestRevision, String manifestSha256,
                             String packageSha256, int fileCount, long sourceTotalSize,
                             String idempotencyKey, int receiptVersion,
                             LocalDateTime backupCompletedAt) {
}
