package com.yigongbao.module.backup.cleanup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 持久化清理评估快照，保证实际删除使用不可变候选集合。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cos_cleanup_snapshot")
public class CleanupSnapshotEntity extends BaseEntity {
    private Long orderId;
    private Long receiptId;
    private String snapshotHash;
    private String objectSetHash;
    private Integer orderStatus;
    private Integer classicCase;
    private String snapshotJson;
}
