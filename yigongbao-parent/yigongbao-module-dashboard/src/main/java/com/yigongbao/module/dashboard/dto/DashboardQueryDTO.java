package com.yigongbao.module.dashboard.dto;

import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 数据概览查询参数 DTO
 */
@Data
public class DashboardQueryDTO {

    /**
     * 时间范围类型
     */
    @NotNull(message = "时间范围不能为空")
    private String timeRange;

    /**
     * 自定义开始日期（timeRange=custom时必填）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /**
     * 自定义结束日期（timeRange=custom时必填）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /**
     * 获取时间范围枚举
     */
    public TimeRangeEnum getTimeRangeEnum() {
        return TimeRangeEnum.fromCode(timeRange);
    }

    /**
     * 验证自定义时间范围参数
     */
    public void validateCustomRange() {
        TimeRangeEnum timeRangeEnum = getTimeRangeEnum();
        if (timeRangeEnum == TimeRangeEnum.CUSTOM) {
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("自定义时间范围需要提供 startDate 和 endDate");
            }
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
        }
    }
}
