package com.yigongbao.module.order.entity.legacy;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("legacy_migration_task")
public class LegacyMigrationTaskEntity extends BaseEntity {
    private String taskType;
    private String status;
    private Long requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime heartbeatAt;
    private LocalDateTime finishedAt;
    private LocalDateTime sourceSnapshotAt;
    private Integer totalRead;
    private Integer insertedCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer errorCount;
    private String errorMessage;
    private String mappingVersion;
}
