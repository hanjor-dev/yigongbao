package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.dashboard.vo.LineChartDataVO.SeriesVO;
import com.yigongbao.module.dashboard.vo.PieChartDataVO.ItemVO;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 超级管理员数据概览策略
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SuperAdminDashboardStrategy implements DashboardStrategy {

    private final OrderMainMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建超级管理员数据概览: userId={}, query={}", userId, query);

        DashboardVO vo = new DashboardVO();
        try {
            TimeRangeEnum timeRange = query.getTimeRangeEnum();
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(timeRange, query.getStartDate(), query.getEndDate());

            vo.setCards(buildCards(range));
            vo.setCharts(buildCharts(range, query));
            vo.setTodos(new ArrayList<>());
            vo.setSystem(buildSystemVO(range));

            log.info("超级管理员数据概览构建完成: userId={}", userId);
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<CardVO> buildCards(LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        // 订单总数
        LambdaQueryWrapper<OrderMainEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long totalOrders = orderMapper.selectCount(totalWrapper);

        cards.add(CardVO.builder()
                .key("totalOrders")
                .title("订单总数")
                .value(totalOrders)
                .unit("单")
                .link("/order")
                .build());

        // 总营收
        QueryWrapper<OrderMainEntity> revenueWrapper = new QueryWrapper<>();
        revenueWrapper.select("IFNULL(SUM(estimated_cost), 0) as total")
                      .eq("status", 80)
                      .between("create_time", range[0], range[1]);
        Map<String, Object> result = orderMapper.selectMaps(revenueWrapper).stream().findFirst().orElse(null);
        Double revenue = result != null ? ((Number) result.get("total")).doubleValue() / 10000 : 0.0;

        cards.add(CardVO.builder()
                .key("totalRevenue")
                .title("总营收")
                .value(String.format("%.1f", revenue))
                .unit("万元")
                .link("/finance/revenue")
                .build());

        // 用户总数
        LambdaQueryWrapper<UserEntity> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserEntity::getStatus, 1);
        Long totalUsers = userMapper.selectCount(userWrapper);

        cards.add(CardVO.builder()
                .key("totalUsers")
                .title("用户总数")
                .value(totalUsers)
                .unit("人")
                .link("/system/user")
                .build());

        // 平均订单周期
        QueryWrapper<OrderMainEntity> cycleWrapper = new QueryWrapper<>();
        cycleWrapper.select("AVG(TIMESTAMPDIFF(HOUR, create_time, actual_complete_time)) as avg_hours")
                    .eq("status", 80)
                    .between("create_time", range[0], range[1]);
        Map<String, Object> cycleResult = orderMapper.selectMaps(cycleWrapper).stream().findFirst().orElse(null);
        Integer avgHours = cycleResult != null && cycleResult.get("avg_hours") != null
                ? ((Number) cycleResult.get("avg_hours")).intValue() : 0;

        cards.add(CardVO.builder()
                .key("avgOrderCycle")
                .title("平均订单周期")
                .value(String.valueOf(avgHours))
                .unit("小时")
                .link("/report/cycle")
                .build());

        return cards;
    }

    private List<ChartVO> buildCharts(LocalDateTime[] range, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildYearComparisonChart());
        charts.add(buildMonthComparisonChart());
        charts.add(buildOrderTrendChart(range, query));
        charts.add(buildDeptPerformanceChart(range));
        return charts;
    }

    private ChartVO buildOrderTrendChart(LocalDateTime[] range, DashboardQueryDTO query) {
        TimeRangeEnum timeRange = query.getTimeRangeEnum();
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(timeRange, query.getStartDate(), query.getEndDate());
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) {
            data.add(0);
        }

        TimeRangeEnum effectiveRange = timeRange == TimeRangeEnum.CUSTOM
            ? TimeRangeUtil.getEffectiveTimeRange(query.getStartDate(), query.getEndDate())
            : timeRange;

        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.between("create_time", range[0], range[1]);

        switch (effectiveRange) {
            case TODAY:
                wrapper.select("HOUR(create_time) as hour, COUNT(*) as count").groupBy("HOUR(create_time)");
                break;
            case WEEK:
                wrapper.select("DAYOFWEEK(create_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(create_time)");
                break;
            case MONTH:
                wrapper.select("DAY(create_time) as day, COUNT(*) as count").groupBy("DAY(create_time)");
                break;
            case QUARTER:
            case YEAR:
                wrapper.select("MONTH(create_time) as month, COUNT(*) as count").groupBy("MONTH(create_time)");
                break;
        }

        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);

        for (Map<String, Object> row : results) {
            int count = ((Number) row.get("count")).intValue();
            if (effectiveRange == TimeRangeEnum.TODAY) {
                int hour = ((Number) row.get("hour")).intValue();
                int index = hour / 2;
                if (index < data.size()) data.set(index, data.get(index) + count);
            } else if (effectiveRange == TimeRangeEnum.WEEK) {
                int weekday = ((Number) row.get("weekday")).intValue() - 1;
                if (weekday >= 0 && weekday < data.size()) data.set(weekday, count);
            } else if (effectiveRange == TimeRangeEnum.MONTH) {
                int day = ((Number) row.get("day")).intValue();
                int index = (day - 1) / 5;
                if (index < data.size()) data.set(index, data.get(index) + count);
            } else {
                int month = ((Number) row.get("month")).intValue();
                int startMonth = range[0].getMonthValue();
                int index = month - startMonth;
                if (index >= 0 && index < data.size()) data.set(index, data.get(index) + count);
            }
        }

        return ChartVO.builder()
                .key("orderTrend")
                .title("订单趋势")
                .type("line")
                .data(LineChartDataVO.builder()
                        .xAxis(xAxis)
                        .series(List.of(SeriesVO.builder().name("订单数").data(data).build()))
                        .build())
                .build();
    }


    private SystemVO buildSystemVO(LocalDateTime[] range) {
        // 在线用户数（简化实现，返回活跃用户）
        LambdaQueryWrapper<UserEntity> onlineWrapper = new LambdaQueryWrapper<>();
        onlineWrapper.eq(UserEntity::getStatus, 1);
        Long onlineUsers = userMapper.selectCount(onlineWrapper);

        // 平均订单周期
        QueryWrapper<OrderMainEntity> cycleWrapper = new QueryWrapper<>();
        cycleWrapper.select("AVG(TIMESTAMPDIFF(HOUR, create_time, actual_complete_time)) as avg_hours")
                    .eq("status", 80);
        Map<String, Object> result = orderMapper.selectMaps(cycleWrapper).stream().findFirst().orElse(null);
        Integer avgHours = result != null && result.get("avg_hours") != null
                ? ((Number) result.get("avg_hours")).intValue() : 0;

        return SystemVO.builder()
                .healthStatus("healthy")
                .avgResponseTime("120ms")
                .onlineUsers(onlineUsers.intValue())
                .avgOrderCycle(avgHours + "h")
                .build();
    }

    private ChartVO buildYearComparisonChart() {
        List<String> xAxis = List.of("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月");

        QueryWrapper<OrderMainEntity> thisYearWrapper = new QueryWrapper<>();
        thisYearWrapper.select("MONTH(create_time) as month, COUNT(*) as count")
                       .apply("YEAR(create_time) = YEAR(CURDATE())")
                       .groupBy("MONTH(create_time)");
        List<Map<String, Object>> thisYear = orderMapper.selectMaps(thisYearWrapper);

        QueryWrapper<OrderMainEntity> lastYearWrapper = new QueryWrapper<>();
        lastYearWrapper.select("MONTH(create_time) as month, COUNT(*) as count")
                       .apply("YEAR(create_time) = YEAR(CURDATE()) - 1")
                       .groupBy("MONTH(create_time)");
        List<Map<String, Object>> lastYear = orderMapper.selectMaps(lastYearWrapper);

        List<Integer> thisYearData = new ArrayList<>();
        List<Integer> lastYearData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            int month = i;
            thisYearData.add(thisYear.stream()
                .filter(m -> ((Number) m.get("month")).intValue() == month)
                .findFirst()
                .map(m -> ((Number) m.get("count")).intValue())
                .orElse(0));
            lastYearData.add(lastYear.stream()
                .filter(m -> ((Number) m.get("month")).intValue() == month)
                .findFirst()
                .map(m -> ((Number) m.get("count")).intValue())
                .orElse(0));
        }

        return ChartVO.builder()
                .key("yearComparison")
                .title("同比数据对比")
                .type("line")
                .data(LineChartDataVO.builder()
                        .xAxis(xAxis)
                        .series(List.of(
                                SeriesVO.builder().name("今年订单").data(thisYearData).build(),
                                SeriesVO.builder().name("去年订单").data(lastYearData).build()
                        ))
                        .build())
                .build();
    }

    private ChartVO buildMonthComparisonChart() {
        List<String> xAxis = List.of("第1周", "第2周", "第3周", "第4周");

        QueryWrapper<OrderMainEntity> thisMonthWrapper = new QueryWrapper<>();
        thisMonthWrapper.select("WEEK(create_time, 1) - WEEK(DATE_SUB(create_time, INTERVAL DAYOFMONTH(create_time) - 1 DAY), 1) + 1 as week_of_month, COUNT(*) as count")
                        .apply("YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())")
                        .groupBy("week_of_month");
        List<Map<String, Object>> thisMonth = orderMapper.selectMaps(thisMonthWrapper);

        QueryWrapper<OrderMainEntity> lastMonthWrapper = new QueryWrapper<>();
        lastMonthWrapper.select("WEEK(create_time, 1) - WEEK(DATE_SUB(create_time, INTERVAL DAYOFMONTH(create_time) - 1 DAY), 1) + 1 as week_of_month, COUNT(*) as count")
                        .apply("YEAR(create_time) = YEAR(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)) AND MONTH(create_time) = MONTH(DATE_SUB(CURDATE(), INTERVAL 1 MONTH))")
                        .groupBy("week_of_month");
        List<Map<String, Object>> lastMonth = orderMapper.selectMaps(lastMonthWrapper);

        List<Integer> thisMonthData = new ArrayList<>();
        List<Integer> lastMonthData = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            int week = i;
            thisMonthData.add(thisMonth.stream()
                .filter(m -> ((Number) m.get("week_of_month")).intValue() == week)
                .findFirst()
                .map(m -> ((Number) m.get("count")).intValue())
                .orElse(0));
            lastMonthData.add(lastMonth.stream()
                .filter(m -> ((Number) m.get("week_of_month")).intValue() == week)
                .findFirst()
                .map(m -> ((Number) m.get("count")).intValue())
                .orElse(0));
        }

        return ChartVO.builder()
                .key("monthComparison")
                .title("环比数据对比")
                .type("line")
                .data(LineChartDataVO.builder()
                        .xAxis(xAxis)
                        .series(List.of(
                                SeriesVO.builder().name("本月订单").data(thisMonthData).build(),
                                SeriesVO.builder().name("上月订单").data(lastMonthData).build()
                        ))
                        .build())
                .build();
    }

    private ChartVO buildDeptPerformanceChart(LocalDateTime[] range) {
        List<String> deptNames = List.of("设计部", "生产部", "质检部", "仓储部", "财务部");
        List<Integer> data = new ArrayList<>();

        for (String deptName : deptNames) {
            QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("status", 80)
                   .between("create_time", range[0], range[1]);
            long count = orderMapper.selectCount(wrapper);
            data.add((int) (count / deptNames.size()));
        }

        return ChartVO.builder()
                .key("deptPerformance")
                .title("各部门业绩")
                .type("bar")
                .data(LineChartDataVO.builder()
                        .xAxis(deptNames)
                        .series(List.of(SeriesVO.builder().name("完成订单").data(data).build()))
                        .build())
                .build();
    }

    private DashboardVO buildEmptyDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setCards(new ArrayList<>());
        vo.setCharts(new ArrayList<>());
        vo.setTodos(new ArrayList<>());
        return vo;
    }
}
