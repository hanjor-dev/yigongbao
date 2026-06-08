package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申请状态枚举
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Getter
@AllArgsConstructor
public enum ApplyStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    EXPIRED(3, "已过期");

    private final Integer code;
    private final String desc;
}
