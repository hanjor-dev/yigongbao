package com.yigongbao.module.notification.announcement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 系统公告接收人快照。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_announcement_recipient")
public class SystemAnnouncementRecipientEntity extends BaseEntity {
    private Long announcementId;
    private Long userId;
    private String userNameSnapshot;
    private String usernameSnapshot;
    private String roleSnapshot;
    private String deliveryStatus;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime revokedAt;

    /** 显式声明，避免不同 Lombok 增量编译环境对该快照字段的访问器解析不一致。 */
    public void setUserNameSnapshot(String userNameSnapshot) {
        this.userNameSnapshot = userNameSnapshot;
    }

    public String getUserNameSnapshot() {
        return userNameSnapshot;
    }

    public void setUsernameSnapshot(String usernameSnapshot) {
        this.usernameSnapshot = usernameSnapshot;
    }

    public String getUsernameSnapshot() {
        return usernameSnapshot;
    }
}
