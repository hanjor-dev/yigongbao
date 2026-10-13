package com.yigongbao.module.notification.announcement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 系统公告审计动作。 */
@Getter
@AllArgsConstructor
public enum AnnouncementAuditOperationEnum {
    PUBLISH("PUBLISH", "发布"),
    REVOKE("REVOKE", "撤回");

    private final String code;
    private final String desc;
}
