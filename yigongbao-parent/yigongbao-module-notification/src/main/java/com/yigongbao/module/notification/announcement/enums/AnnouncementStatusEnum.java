package com.yigongbao.module.notification.announcement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 系统公告生命周期状态。 */
@Getter
@AllArgsConstructor
public enum AnnouncementStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    REVOKED("REVOKED", "已撤回");

    private final String code;
    private final String desc;
}
