package com.yigongbao.module.notification.announcement.dto;

import lombok.Data;

@Data
public class AnnouncementPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String title;
    private String status;
    private String targetType;
}
