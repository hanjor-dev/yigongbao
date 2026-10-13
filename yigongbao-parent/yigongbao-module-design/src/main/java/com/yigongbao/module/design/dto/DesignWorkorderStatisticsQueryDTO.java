package com.yigongbao.module.design.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 设计工单统计查询条件（不含分页和状态条件）。 */
@Data
public class DesignWorkorderStatisticsQueryDTO {
    private String orderCode;
    private Integer isUrgent;
    private Long hospitalId;
    private String businessType;
    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;
}
