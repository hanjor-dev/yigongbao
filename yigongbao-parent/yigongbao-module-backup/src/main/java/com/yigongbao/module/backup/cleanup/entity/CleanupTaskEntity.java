package com.yigongbao.module.backup.cleanup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 持久化 COS 清理任务主记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cos_cleanup_task")
public class CleanupTaskEntity extends BaseEntity {
    private String taskNo;
    private String idempotencyKey;
    private Long policyId;
    private Long snapshotId;
    private String mode;
    private String taskStatus;
    private Long estimatedVersionBytes;
    private Long actualPurgedBytes;
    private String failureCode;
    private String failureMessage;
    private Integer version;
}
