package com.yigongbao.module.production.qc.dto;

import lombok.Data;

@Data
public class ProductionRedoPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Long recordId;
}
