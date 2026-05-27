package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工序状态枚举
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
public enum ProcessStatusEnum {
    PENDING("pending", "待开始"),
    IN_PROGRESS("in_progress", "进行中"),
    COMPLETED("completed", "已完成");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
