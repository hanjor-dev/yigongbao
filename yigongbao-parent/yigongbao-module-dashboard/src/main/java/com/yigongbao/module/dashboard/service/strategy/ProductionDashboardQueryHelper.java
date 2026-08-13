package com.yigongbao.module.dashboard.service.strategy;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

final class ProductionDashboardQueryHelper {

    private ProductionDashboardQueryHelper() {
    }

    record Range(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        long days() {
            return ChronoUnit.DAYS.between(startInclusive.toLocalDate(), endExclusive.toLocalDate());
        }
    }

    static Range range(DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndExclusiveTime(
                query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        return new Range(range[0], range[1]);
    }

    static String groupSelect(DashboardQueryDTO query, Range range, String column) {
        TimeRangeEnum timeRange = query.getTimeRangeEnum();
        return switch (timeRange) {
            case TODAY -> "FLOOR(HOUR(" + column + ") / 2)";
            case WEEK -> "WEEKDAY(" + column + ")";
            case MONTH -> "FLOOR((DAY(" + column + ") - 1) / 5)";
            case QUARTER, YEAR -> "PERIOD_DIFF(DATE_FORMAT(" + column + ", '%Y%m'), '"
                    + range.startInclusive().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "')";
            case CUSTOM -> customGroupSelect(range, column);
        };
    }

    private static String customGroupSelect(Range range, String column) {
        if (range.days() <= 1) {
            return "FLOOR(HOUR(" + column + ") / 2)";
        }
        if (range.days() <= 31) {
            return "DATEDIFF(" + column + ", '" + range.startInclusive().toLocalDate() + "')";
        }
        return "PERIOD_DIFF(DATE_FORMAT(" + column + ", '%Y%m'), '"
                + range.startInclusive().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "')";
    }

    static int bucketIndex(DashboardQueryDTO query, Range range, int timeUnit) {
        if (query.getTimeRangeEnum() == TimeRangeEnum.CUSTOM && range.days() > 7 && range.days() <= 31) {
            return timeUnit / 5;
        }
        return timeUnit;
    }
}
