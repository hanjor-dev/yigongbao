package com.yigongbao.module.notification.announcement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 系统公告接收状态。 */
@Getter
@AllArgsConstructor
public enum AnnouncementRecipientStatusEnum {
    PENDING("PENDING", "待确认"),
    ACKNOWLEDGED("ACKNOWLEDGED", "已确认"),
    REVOKED("REVOKED", "已撤回");

    private final String code;
    private final String desc;
}
