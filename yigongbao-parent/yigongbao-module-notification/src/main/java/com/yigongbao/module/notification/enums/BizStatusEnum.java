package com.yigongbao.module.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态枚举
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum BizStatusEnum {

    PENDING("PENDING", "待处理"),
    PROCESSED("PROCESSED", "已处理"),
    CLAIMED("CLAIMED", "已被他人接收");

    private final String code;
    private final String desc;
}
