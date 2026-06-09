package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单自定义导出 DTO
 * 用户可选择时间范围和导出字段
 *
 * @author hanjor
 * @date 2026-06-09
 */
@Data
public class OrderCustomExportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建时间起始（必填）
     */
    @NotNull(message = "创建时间起始不能为空")
    private LocalDateTime createTimeStart;

    /**
     * 创建时间结束（必填）
     */
    @NotNull(message = "创建时间结束不能为空")
    private LocalDateTime createTimeEnd;

    /**
     * 导出字段列表（必填，至少1个字段）
     * 字段key对应OrderListVO的字段，如：orderCode, hospitalName, patientName等
     */
    @NotEmpty(message = "导出字段列表不能为空")
    private List<String> exportFields;

    /**
     * 字段显示名称映射（可选）
     * key=字段key, value=显示名称
     * 前端可传入自定义列名，如：{"orderCode": "订单号", "hospitalName": "医院"}
     */
    private java.util.Map<String, String> fieldLabels;
}
