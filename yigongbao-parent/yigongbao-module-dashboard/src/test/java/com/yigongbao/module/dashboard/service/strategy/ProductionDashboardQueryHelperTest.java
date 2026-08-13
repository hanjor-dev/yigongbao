package com.yigongbao.module.dashboard.service.strategy;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionDashboardQueryHelperTest {

    @Test
    void weekBucketsStartOnMondayAndEndOnSunday() {
        DashboardQueryDTO query = query("week", null, null);
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);

        assertThat(ProductionDashboardQueryHelper.groupSelect(query, range, "create_time"))
                .isEqualTo("WEEKDAY(create_time)");
        assertThat(ProductionDashboardQueryHelper.bucketIndex(query, range, 0)).isZero();
        assertThat(ProductionDashboardQueryHelper.bucketIndex(query, range, 6)).isEqualTo(6);
    }

    @Test
    void monthDayThirtyFallsIntoLastFiveDayBucket() {
        DashboardQueryDTO query = query("month", null, null);
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);

        assertThat(ProductionDashboardQueryHelper.groupSelect(query, range, "create_time"))
                .isEqualTo("FLOOR((DAY(create_time) - 1) / 5)");
        assertThat(ProductionDashboardQueryHelper.bucketIndex(query, range, 5)).isEqualTo(5);
    }

    @Test
    void customDailyBucketUsesRelativeDayAcrossMonthBoundary() {
        DashboardQueryDTO query = query("custom", LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 3));
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);

        assertThat(ProductionDashboardQueryHelper.groupSelect(query, range, "create_time"))
                .contains("DATEDIFF(create_time, '2026-07-30')");
        assertThat(ProductionDashboardQueryHelper.bucketIndex(query, range, 4)).isEqualTo(4);
    }

    @Test
    void customMonthlyBucketUsesRelativeMonthAcrossYearBoundary() {
        DashboardQueryDTO query = query("custom", LocalDate.of(2025, 11, 15), LocalDate.of(2026, 2, 20));
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);

        assertThat(ProductionDashboardQueryHelper.groupSelect(query, range, "create_time"))
                .contains("PERIOD_DIFF").contains("202511");
        assertThat(ProductionDashboardQueryHelper.bucketIndex(query, range, 3)).isEqualTo(3);
    }

    @Test
    void rangeIsLeftClosedAndRightOpen() {
        DashboardQueryDTO query = query("custom", LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 13));
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);

        assertThat(range.startInclusive()).isEqualTo(LocalDateTime.of(2026, 8, 13, 0, 0));
        assertThat(range.endExclusive()).isEqualTo(LocalDateTime.of(2026, 8, 14, 0, 0));
    }

    private DashboardQueryDTO query(String timeRange, LocalDate start, LocalDate end) {
        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setTimeRange(timeRange);
        query.setStartDate(start);
        query.setEndDate(end);
        return query;
    }
}
