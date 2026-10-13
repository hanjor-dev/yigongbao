package com.yigongbao.module.production.record.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 生产流转卡统计查询条件（不含分页和状态条件）。 */
@Data
public class ProductionRecordStatisticsQueryDTO {
    private String keyword;
    private Long processingCenterId;
    private LocalDateTime orderCreateTimeStart;
    private LocalDateTime orderCreateTimeEnd;
}
