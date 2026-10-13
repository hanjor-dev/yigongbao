package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

/** 发布时解析出的有效用户快照。 */
@Data
public class AnnouncementTargetUserVO {
    private Long userId;
    private String username;
    private String realName;
    private String roleName;
}
