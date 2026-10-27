package com.yigongbao.module.backup.receipt.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_backup_receipt")
public class OrderBackupReceiptEntity extends BaseEntity {
    private Long orderId;
    private String manifestRevision;
    private String manifestSha256;
    private String packageSha256;
    private Integer fileCount;
    private Long sourceTotalSize;
    private String localIdempotencyKey;
    private Integer receiptVersion;
    private Long previousReceiptId;
    private Integer isCurrent;
    private Integer stateVersion;
    private LocalDateTime backupCompletedAt;
    private String receiptStatus;
    private LocalDateTime lastVerifiedAt;
    private String verificationSource;
    private LocalDateTime invalidatedAt;
    private String invalidatedReason;
}
