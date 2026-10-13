package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存设计工单列配置参数
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class SaveDesignColumnConfigDTO {

    @NotNull(message = "列配置不能为空")
    private List<ColumnItemDTO> columns;

    @Data
    public static class ColumnItemDTO {

        @NotBlank(message = "字段名不能为空")
        private String field;

        @NotBlank(message = "列标题不能为空")
        private String label;

        @NotNull(message = "是否可见不能为空")
        private Boolean visible;

        @NotNull(message = "排序序号不能为空")
        private Integer sort;

        private Integer width;

        /** 固定位置：left / right / null */
        private String fixed;
    }
}
