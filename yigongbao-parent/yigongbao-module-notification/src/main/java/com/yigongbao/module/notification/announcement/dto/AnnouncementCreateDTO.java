package com.yigongbao.module.notification.announcement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnnouncementCreateDTO {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String contentHtml;

    @Valid
    @NotNull(message = "公告目标不能为空")
    private AnnouncementTargetDTO target;

    @Valid
    private List<AnnouncementAttachmentDTO> attachments = new ArrayList<>();
}
