package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

/** 公告阅读确认统计。 */
@Data
public class AnnouncementStatisticsVO {
    private Long targetCount;
    private Long pendingCount;
    private Long acknowledgedCount;
    private Long revokedCount;
    private Integer acknowledgeRate;
}
