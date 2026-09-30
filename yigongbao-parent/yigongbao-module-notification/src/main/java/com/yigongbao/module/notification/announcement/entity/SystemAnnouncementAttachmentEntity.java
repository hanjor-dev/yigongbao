package com.yigongbao.module.notification.announcement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统公告附件关联。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_announcement_attachment")
public class SystemAnnouncementAttachmentEntity extends BaseEntity {
    private Long announcementId;
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer sort;
}
