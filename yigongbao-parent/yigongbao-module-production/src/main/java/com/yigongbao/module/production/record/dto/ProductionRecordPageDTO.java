package com.yigongbao.module.production.record.dto;

import lombok.Data;

/**
 * 生产流转卡分页查询 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionRecordPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String recordNo;
    private String status;
    private Long processingCenterId;
}
