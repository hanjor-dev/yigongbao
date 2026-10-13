package com.yigongbao.module.notification.announcement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 系统公告主记录。发布后正文和目标字段不可变。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_announcement")
public class SystemAnnouncementEntity extends BaseEntity {
    private String title;
    private String contentHtml;
    private String contentText;
    private String status;
    private String targetType;
    private String targetRoleIds;
    private String targetUserIds;
    private Integer forceConfirm;
    private Integer targetCount;
    private Integer acknowledgedCount;
    private Integer revokedCount;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private LocalDateTime revokedAt;
    private Long revokedBy;
}
