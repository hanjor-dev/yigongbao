package com.yigongbao.module.backup.cleanup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 持久化 COS 删除授权及其生命周期。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cos_delete_permit")
public class CleanupPermitEntity extends BaseEntity {
    private String permitId;
    private Long cleanupTaskId;
    private Integer attemptNo;
    private String permitStatus;
    private String cleanupSnapshotHash;
    private String objectSetHash;
    private Long fencingToken;
    private Long receiptId;
    private Integer receiptVersion;
    private String packageHash;
    private String nonce;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Integer version;
}
