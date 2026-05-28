package com.yigongbao.module.production.qc.dto;

import lombok.Data;

/**
 * redo 产品分页查询 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionRedoPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    /** 按流转卡ID筛选（可选） */
    private Long recordId;
}
