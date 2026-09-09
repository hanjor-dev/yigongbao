package com.yigongbao.module.design.vo;

import lombok.Data;

/** 设计工单统计结果。 */
@Data
public class DesignWorkorderStatisticsVO {
    private Long total = 0L;
    private Long pendingDesign = 0L;
    private Long designing = 0L;
    private Long designCompleted = 0L;
}
