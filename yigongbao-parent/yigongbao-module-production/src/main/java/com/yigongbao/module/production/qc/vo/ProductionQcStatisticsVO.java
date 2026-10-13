package com.yigongbao.module.production.qc.vo;

import lombok.Data;

/** 质检统计结果。 */
@Data
public class ProductionQcStatisticsVO {
    private Long qcInProgress = 0L;
    private Long qcCompleted = 0L;
}
