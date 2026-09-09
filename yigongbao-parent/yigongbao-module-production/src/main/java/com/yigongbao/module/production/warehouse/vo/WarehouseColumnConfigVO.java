package com.yigongbao.module.production.warehouse.vo;

import lombok.Data;

import java.util.List;

/**
 * 仓储列表列配置 VO
 */
@Data
public class WarehouseColumnConfigVO {

    /** 后端列配置结构版本，前端无需参与维护。 */
    private Integer version;

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
