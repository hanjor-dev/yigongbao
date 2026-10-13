package com.yigongbao.module.notification.announcement.dto;

import lombok.Data;

/** 当前用户公告历史分页条件。 */
@Data
public class AnnouncementHistoryPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String deliveryStatus;
}
