package com.yigongbao.module.notification.dto;

import com.yigongbao.module.notification.enums.MessageCategoryEnum;
import com.yigongbao.module.notification.enums.MessageTypeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 通知发送 DTO
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
@Builder
public class NotificationDTO {

    private String title;
    private String content;
    private MessageTypeEnum messageType;
    private MessageCategoryEnum category;
    private String bizType;
    private Long bizId;
    private String bizData;
    private String jumpUrl;
}
