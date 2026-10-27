package com.yigongbao.module.backup.cleanup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 持久化清理任务中的单个 COS 对象及其执行结果。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cos_cleanup_item")
public class CleanupItemEntity extends BaseEntity {
    private Long cleanupTaskId;
    private Long fileId;
    private String platform;
    private String bucket;
    private String objectKey;
    private String versionIdentity;
    private Integer sharedReferenceCount;
    private String deleteStatus;
    private Long versionBytes;
    private Integer retryCount;
    private String lastError;
    private Integer version;
}
