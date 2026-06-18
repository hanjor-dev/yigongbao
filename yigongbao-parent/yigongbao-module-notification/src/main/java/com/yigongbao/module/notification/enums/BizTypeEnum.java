package com.yigongbao.module.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum {

    ORDER("ORDER", "订单"),
    MODIFY_APPLY("MODIFY_APPLY", "修改申请"),
    PRODUCTION_CARD("PRODUCTION_CARD", "生产卡");

    private final String code;
    private final String desc;
}
