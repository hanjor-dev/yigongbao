package com.yigongbao.module.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 修改申请状态枚举
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Getter
@AllArgsConstructor
public enum ApplyStatusEnum {

    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    EXPIRED(4, "已过期");

    private final Integer code;
    private final String desc;
}
