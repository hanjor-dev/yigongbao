package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 当前用户可查看的公告历史记录。 */
@Data
public class AnnouncementHistoryVO {
    private Long id;
    private String title;
    private String contentHtml;
    private String contentText;
    private Integer forceConfirm;
    private String status;
    private String deliveryStatus;
    private LocalDateTime publishedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime revokedAt;
    private List<AnnouncementAttachmentVO> attachments;
}
