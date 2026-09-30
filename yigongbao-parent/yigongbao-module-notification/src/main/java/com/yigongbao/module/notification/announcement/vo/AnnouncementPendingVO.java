package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementPendingVO {
    private Long id;
    private String title;
    private String contentHtml;
    private Integer forceConfirm;
    private LocalDateTime publishedAt;
    private List<AnnouncementAttachmentVO> attachments;
}
