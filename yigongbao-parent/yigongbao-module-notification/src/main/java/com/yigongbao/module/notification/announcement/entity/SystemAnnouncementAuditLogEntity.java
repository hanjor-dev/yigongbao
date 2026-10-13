package com.yigongbao.module.notification.announcement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 系统公告发布和撤回审计日志。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_announcement_audit_log")
public class SystemAnnouncementAuditLogEntity extends BaseEntity {
    private Long announcementId;
    private String operationType;
    private Long operatorId;
    private String operatorName;
    private String beforeStatus;
    private String afterStatus;
    private String targetType;
    private Integer targetCount;
    private String clientIp;
    private String remark;
    private LocalDateTime operationTime;
}
