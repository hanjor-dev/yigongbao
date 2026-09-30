package com.yigongbao.module.notification.announcement.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 公告接收人快照明细。 */
@Data
public class AnnouncementRecipientVO {
    private Long id;
    private Long announcementId;
    private Long userId;
    private String userNameSnapshot;
    private String usernameSnapshot;
    private String roleSnapshot;
    private String deliveryStatus;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime revokedAt;

    public void setUserNameSnapshot(String userNameSnapshot) {
        this.userNameSnapshot = userNameSnapshot;
    }

    public void setUsernameSnapshot(String usernameSnapshot) {
        this.usernameSnapshot = usernameSnapshot;
    }
}
