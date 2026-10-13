package com.yigongbao.module.notification.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementAttachmentDTO {
    /** 文件服务返回的雪花 ID 使用字符串承载，避免前端 JavaScript 数值精度丢失。 */
    private String fileId;
    @NotBlank(message = "附件名称不能为空")
    private String fileName;
    @NotBlank(message = "附件地址不能为空")
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer sort = 0;
}
