package com.yigongbao.module.notification.announcement.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/** 公告接收人快照明细。 */
@Data
public class AnnouncementRecipientVO {
    private Long id;
    private Long announcementId;
    private Long userId;
    /** 姓名字段：明确映射 system_announcement_recipient.user_name_snapshot。 */
    @JsonProperty("realName")
    private String realName;
    @JsonProperty("userNameSnapshot")
    private String userNameSnapshot;
    @JsonProperty("usernameSnapshot")
    private String usernameSnapshot;
    @JsonProperty("roleSnapshot")
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
