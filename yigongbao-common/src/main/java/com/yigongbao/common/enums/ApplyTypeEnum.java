package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申请类型枚举
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Getter
@AllArgsConstructor
public enum ApplyTypeEnum {

    FULL(1, "全量修改");

    private final Integer code;
    private final String desc;
}
