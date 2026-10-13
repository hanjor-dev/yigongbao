package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

@Data
public class AnnouncementAttachmentVO {
    private Long id;
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer sort;
}
