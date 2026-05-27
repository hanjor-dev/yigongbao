package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流转卡状态枚举
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Getter
@AllArgsConstructor
public enum RecordStatusEnum {
    PENDING_PRINT("pending_print", "待打印"),
    PRINTING("printing", "打印中"),
    PRINT_COMPLETED("print_completed", "打印完成"),
    POST_PROCESSING("post_processing", "后处理中"),
    QC_IN_PROGRESS("qc_in_progress", "质检中"),
    PACKING("packing", "包装中"),
    WAREHOUSE_IN("warehouse_in", "入库中"),
    COMPLETED("completed", "已完成"),
    PRINT_FAILED("print_failed", "打印失败"),
    ABANDONED("abandoned", "已废弃");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
