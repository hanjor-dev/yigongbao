package com.yigongbao.module.production.warehouse.vo;

import lombok.Data;

/** 仓储产品状态统计结果。 */
@Data
public class WarehouseStatisticsVO {
    private Long total = 0L;
    private Long pendingWarehouseIn = 0L;
    private Long warehoused = 0L;
    private Long warehouseOut = 0L;
}
