package com.yigongbao.module.production.warehouse.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 仓储列表查询参数
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Data
public class ListWarehouseDTO {
    private String keyword;
    private String status;
    private LocalDateTime warehouseInTimeStart;
    private LocalDateTime warehouseInTimeEnd;
    private LocalDateTime warehouseOutTimeStart;
    private LocalDateTime warehouseOutTimeEnd;
    private Integer page;
    private Integer size;
}
