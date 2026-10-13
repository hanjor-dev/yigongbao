package com.yigongbao.module.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    MESSAGE("MESSAGE", "普通消息"),
    POPUP("POPUP", "弹窗通知");

    private final String code;
    private final String desc;
}
