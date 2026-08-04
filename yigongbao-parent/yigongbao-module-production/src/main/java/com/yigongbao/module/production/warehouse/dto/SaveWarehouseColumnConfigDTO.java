package com.yigongbao.module.production.warehouse.dto;

import com.yigongbao.module.production.util.ColumnConfigItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * 保存仓储列表列配置参数
 */
@Data
public class SaveWarehouseColumnConfigDTO {

    @NotNull(message = "列配置不能为空")
    private List<@Valid ColumnItemDTO> columns;

    @Data
    public static class ColumnItemDTO implements ColumnConfigItem {
        @NotBlank(message = "字段名不能为空")
        private String field;

        @NotBlank(message = "列标题不能为空")
        private String label;

        @NotNull(message = "是否可见不能为空")
        private Boolean visible;

        @NotNull(message = "排序序号不能为空")
        @Positive(message = "排序序号必须为正数")
        private Integer sort;

        @Positive(message = "列宽度必须为正数")
        private Integer width;

        @Pattern(regexp = "left|right", message = "固定位置只能是 left 或 right")
        private String fixed;
    }
}
