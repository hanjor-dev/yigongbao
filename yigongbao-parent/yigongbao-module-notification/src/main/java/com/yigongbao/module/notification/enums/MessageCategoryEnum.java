package com.yigongbao.module.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息分类枚举
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum MessageCategoryEnum {

    ORDER("ORDER", "订单"),
    APPROVAL("APPROVAL", "审核"),
    DESIGN("DESIGN", "设计"),
    PRODUCTION("PRODUCTION", "生产"),
    SYSTEM("SYSTEM", "系统通知");

    private final String code;
    private final String desc;
}
