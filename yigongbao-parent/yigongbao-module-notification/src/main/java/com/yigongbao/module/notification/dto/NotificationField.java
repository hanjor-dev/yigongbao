package com.yigongbao.module.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 消息卡片字段：key（标识）、label（中文显示名）、value（值）
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
@AllArgsConstructor
public class NotificationField {
    private String key;
    private String label;
    private String value;
}
