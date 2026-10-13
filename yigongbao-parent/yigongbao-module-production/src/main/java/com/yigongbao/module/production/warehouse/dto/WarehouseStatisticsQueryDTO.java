package com.yigongbao.module.production.warehouse.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 仓储统计查询条件（不含分页和产品状态条件）。 */
@Data
public class WarehouseStatisticsQueryDTO {
    private String keyword;
    private LocalDateTime warehouseInTimeStart;
    private LocalDateTime warehouseInTimeEnd;
    private LocalDateTime warehouseOutTimeStart;
    private LocalDateTime warehouseOutTimeEnd;
}
