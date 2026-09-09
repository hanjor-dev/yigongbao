package com.yigongbao.module.production.record.vo;

import lombok.Data;

/** 生产流转卡状态统计结果。 */
@Data
public class ProductionRecordStatisticsVO {
    private Long total = 0L;
    private Long designCompleted = 0L;
    private Long pendingPrint = 0L;
    private Long printing = 0L;
    private Long printCompleted = 0L;
    private Long printFailed = 0L;
    private Long postProcessing = 0L;
    private Long qcInProgress = 0L;
    private Long qcCompleted = 0L;
    private Long qcPassed = 0L;
    private Long qcFailed = 0L;
    private Long rework = 0L;
    private Long packing = 0L;
    private Long pendingWarehouseIn = 0L;
    private Long warehoused = 0L;
    private Long warehouseOut = 0L;
    private Long completed = 0L;
}
