package com.yigongbao.module.notification.announcement.dto;

import lombok.Data;

/** 公告接收人明细分页条件。 */
@Data
public class AnnouncementRecipientPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String deliveryStatus;
    private String keyword;
}
