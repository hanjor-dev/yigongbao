package com.yigongbao.module.production.qc.vo;

import lombok.Data;

import java.util.List;

/**
 * 质检列表列配置 VO
 */
@Data
public class QcColumnConfigVO {

    private String module = "quality";

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
