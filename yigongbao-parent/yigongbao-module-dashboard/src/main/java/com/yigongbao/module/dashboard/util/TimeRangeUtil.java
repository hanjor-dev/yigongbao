package com.yigongbao.module.dashboard.util;

import com.yigongbao.module.dashboard.enums.TimeRangeEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间范围计算工具类
 */
public class TimeRangeUtil {

    /**
     * 获取时间范围的起止时间
     * @param timeRange 时间范围枚举
     * @param startDate 自定义开始日期
     * @param endDate 自定义结束日期
     * @return [startTime, endTime]
     */
    public static LocalDateTime[] getStartAndEndTime(TimeRangeEnum timeRange, LocalDate startDate, LocalDate endDate) {
        if (timeRange == TimeRangeEnum.CUSTOM) {
            return new LocalDateTime[]{
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            };
        }
        return getStartAndEndTime(timeRange);
    }

    /**
     * 获取时间范围的起止时间（预定义范围）
     * @return [startTime, endTime]
     */
    public static LocalDateTime[] getStartAndEndTime(TimeRangeEnum timeRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end;

        switch (timeRange) {
            case TODAY:
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;

            case WEEK:
                start = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                           .toLocalDate().atStartOfDay();
                end = now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
                         .toLocalDate().atTime(23, 59, 59);
                break;

            case MONTH:
                start = now.with(TemporalAdjusters.firstDayOfMonth())
                           .toLocalDate().atStartOfDay();
                end = now.with(TemporalAdjusters.lastDayOfMonth())
                         .toLocalDate().atTime(23, 59, 59);
                break;

            case QUARTER:
                int currentMonth = now.getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                start = now.withMonth(quarterStartMonth).withDayOfMonth(1)
                           .toLocalDate().atStartOfDay();
                end = start.plusMonths(3).minusDays(1).toLocalDate().atTime(23, 59, 59);
                break;

            case YEAR:
                start = now.with(TemporalAdjusters.firstDayOfYear())
                           .toLocalDate().atStartOfDay();
                end = now.with(TemporalAdjusters.lastDayOfYear())
                         .toLocalDate().atTime(23, 59, 59);
                break;

            default:
                throw new IllegalArgumentException("不支持的时间范围: " + timeRange);
        }

        return new LocalDateTime[]{start, end};
    }

    /**
     * 生成 X 轴标签
     */
    public static List<String> getXAxisLabels(TimeRangeEnum timeRange, LocalDate startDate, LocalDate endDate) {
        if (timeRange == TimeRangeEnum.CUSTOM) {
            return generateCustomXAxisLabels(startDate, endDate);
        }
        return getXAxisLabels(timeRange);
    }

    /**
     * 生成 X 轴标签（预定义范围）
     */
    public static List<String> getXAxisLabels(TimeRangeEnum timeRange) {
        List<String> labels = new ArrayList<>();

        switch (timeRange) {
            case TODAY:
                for (int i = 0; i < 24; i += 2) {
                    labels.add(i + "时");
                }
                break;

            case WEEK:
                labels.add("周一");
                labels.add("周二");
                labels.add("周三");
                labels.add("周四");
                labels.add("周五");
                labels.add("周六");
                labels.add("周日");
                break;

            case MONTH:
                LocalDateTime[] range = getStartAndEndTime(timeRange);
                int month = range[0].getMonthValue();
                int lastDay = range[1].getDayOfMonth();
                for (int day = 1; day <= lastDay; day += 5) {
                    labels.add(month + "月" + day + "日");
                }
                break;

            case QUARTER:
                LocalDateTime[] qRange = getStartAndEndTime(timeRange);
                int startMonth = qRange[0].getMonthValue();
                for (int i = 0; i < 3; i++) {
                    labels.add((startMonth + i) + "月");
                }
                break;

            case YEAR:
                for (int i = 1; i <= 12; i++) {
                    labels.add(i + "月");
                }
                break;
        }

        return labels;
    }

    /**
     * 生成自定义时间范围的 X 轴标签（根据跨度自动选择粒度）
     */
    private static List<String> generateCustomXAxisLabels(LocalDate startDate, LocalDate endDate) {
        List<String> labels = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (days <= 1) {
            // 1天：按2小时
            for (int i = 0; i < 24; i += 2) {
                labels.add(i + "时");
            }
        } else if (days <= 7) {
            // 2-7天：按天
            LocalDate date = startDate;
            while (!date.isAfter(endDate)) {
                labels.add(date.getMonthValue() + "月" + date.getDayOfMonth() + "日");
                date = date.plusDays(1);
            }
        } else if (days <= 31) {
            // 8-31天：每5天
            for (int day = 1; day <= days; day += 5) {
                LocalDate date = startDate.plusDays(day - 1);
                labels.add(date.getMonthValue() + "月" + date.getDayOfMonth() + "日");
            }
        } else {
            // >31天：按月
            LocalDate date = startDate.withDayOfMonth(1);
            while (!date.isAfter(endDate)) {
                labels.add(date.getMonthValue() + "月");
                date = date.plusMonths(1);
            }
        }

        return labels;
    }

    /**
     * 根据自定义时间范围判断应使用的数据聚合粒度
     */
    public static TimeRangeEnum getEffectiveTimeRange(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 1) return TimeRangeEnum.TODAY;
        if (days <= 7) return TimeRangeEnum.WEEK;
        if (days <= 31) return TimeRangeEnum.MONTH;
        return TimeRangeEnum.YEAR;
    }
}
