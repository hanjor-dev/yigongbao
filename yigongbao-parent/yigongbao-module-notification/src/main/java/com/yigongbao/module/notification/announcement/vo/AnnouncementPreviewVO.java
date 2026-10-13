package com.yigongbao.module.notification.announcement.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnnouncementPreviewVO {
    private String title;
    private String contentHtml;
    private String contentText;
}
