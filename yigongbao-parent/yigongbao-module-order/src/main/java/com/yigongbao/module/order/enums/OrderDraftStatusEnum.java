package com.yigongbao.module.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单草稿状态枚举
 *
 * @author hanjor
 * @date 2026-05-13
 */
@Getter
@AllArgsConstructor
public enum OrderDraftStatusEnum {

    VALID(1, "有效"),
    SUBMITTED(2, "已提交"),
    EXPIRED(3, "已过期");

    private final int code;
    private final String name;
}
