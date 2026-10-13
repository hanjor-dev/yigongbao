package com.yigongbao.module.notification.announcement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 系统公告目标类型。 */
@Getter
@AllArgsConstructor
public enum AnnouncementTargetTypeEnum {
    ALL("ALL", "全员"),
    ROLE("ROLE", "按角色"),
    USER("USER", "指定账户");

    private final String code;
    private final String desc;
}
