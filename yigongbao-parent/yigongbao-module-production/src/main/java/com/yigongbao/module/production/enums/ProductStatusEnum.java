package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生产产品状态枚举
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {
    PENDING("pending", "待生产"),
    IN_PROCESS("in_process", "生产中"),
    FAIL("fail", "质检不合格"),
    PASS("pass", "质检合格"),
    PENDING_WAREHOUSE_IN("pending_warehouse_in", "待入库"),
    WAREHOUSED("warehoused", "已入库"),
    WAREHOUSE_OUT("warehouse_out", "已出库"),
    COMPLETED("completed", "已完成"),
    CANCELLED("cancelled", "已废弃");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
