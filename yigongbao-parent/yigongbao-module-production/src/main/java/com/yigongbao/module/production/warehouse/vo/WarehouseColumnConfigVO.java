package com.yigongbao.module.production.warehouse.vo;

import lombok.Data;

import java.util.List;

/**
 * 仓储列表列配置 VO
 */
@Data
public class WarehouseColumnConfigVO {

    private String module = "warehouse";

    private List<ColumnItemVO> columns;

    @Data
    public static class ColumnItemVO {
        private String field;
        private String label;
        private Boolean visible;
        private Integer sort;
        private Integer width;
        private String fixed;
    }
}
