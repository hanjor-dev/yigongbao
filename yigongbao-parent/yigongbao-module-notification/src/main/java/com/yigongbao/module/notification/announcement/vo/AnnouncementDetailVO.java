package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端公告详情，包含草稿目标快照和附件。 */
@Data
public class AnnouncementDetailVO {
    private Long id;
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
    private LocalDateTime createTime;
    private Long createBy;
    private LocalDateTime updateTime;
    private Long updateBy;
    private List<AnnouncementAttachmentVO> attachments;
}
