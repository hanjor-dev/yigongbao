package com.yigongbao.module.notification.announcement.dto;

import com.yigongbao.module.notification.announcement.enums.AnnouncementTargetTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnnouncementTargetDTO {
    @NotNull(message = "公告目标类型不能为空")
    private AnnouncementTargetTypeEnum targetType;
    private List<Long> roleIds = new ArrayList<>();
    private List<Long> userIds = new ArrayList<>();
}
