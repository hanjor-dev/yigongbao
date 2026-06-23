package com.yigongbao.module.dashboard.service.strategy;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinanceDashboardStrategy implements DashboardStrategy {
    private final OrderMainMapper orderMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建财务数据概览: userId={}, query={}", userId, query);
        TimeRangeEnum timeRange = query.getTimeRangeEnum();
        try {
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(timeRange, query.getStartDate(), query.getEndDate());
            DashboardVO vo = new DashboardVO();
            vo.setCards(buildCards(range));
            vo.setCharts(buildCharts(range, timeRange));
            vo.setTodos(List.of(
                TodoVO.builder().id(1).title("逾期账款").count(0).link("/receivable?status=overdue").urgent(true).build(),
                TodoVO.builder().id(2).title("待开发票").count(0).link("/invoice?status=pending").build()
            ));
            return vo;
        } catch (Exception e) {
            log.error("构建财务数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
    }

    private List<CardVO> buildCards(LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("IFNULL(SUM(estimated_cost), 0) as total")
               .eq("status", 80).between("create_time", range[0], range[1]);
        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);
        double revenue = 0.0;
        if (results != null && !results.isEmpty()) {
            Map<String, Object> result = results.get(0);
            if (result != null && result.containsKey("total")) {
                revenue = ((Number) result.getOrDefault("total", 0)).doubleValue() / 10000;
            }
        }

        return List.of(
            CardVO.builder().key("totalRevenue").title("总营收").value(String.format("%.2f", revenue)).unit("万元").build(),
            CardVO.builder().key("receivableAmount").title("应收账款").value("0.0").unit("万元").build(),
            CardVO.builder().key("receivedAmount").title("已收账款").value("0.0").unit("万元").build(),
            CardVO.builder().key("overdueAmount").title("逾期账款").value("0.0").unit("万元").build()
        );
    }

    private List<ChartVO> buildCharts(LocalDateTime[] range, TimeRangeEnum timeRange) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildRevenueTrendChart(range, timeRange));
        charts.add(buildPaymentStatusChart());
        charts.add(buildDeptRevenueChart(range));
        return charts;
    }

    private ChartVO buildRevenueTrendChart(LocalDateTime[] range, TimeRangeEnum timeRange) {
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(timeRange);
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) data.add(0);

        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 80);
        switch (timeRange) {
            case TODAY:
                wrapper.select("HOUR(create_time) as hour, IFNULL(SUM(estimated_cost), 0) / 10000 as revenue")
                       .apply("DATE(create_time) = CURDATE()").groupBy("HOUR(create_time)");
                break;
            case WEEK:
                wrapper.select("DAYOFWEEK(create_time) as weekday, IFNULL(SUM(estimated_cost), 0) / 10000 as revenue")
                       .apply("YEARWEEK(create_time) = YEARWEEK(NOW())").groupBy("DAYOFWEEK(create_time)");
                break;
            default:
                wrapper.select("DATE(create_time) as date, IFNULL(SUM(estimated_cost), 0) / 10000 as revenue")
                       .between("create_time", range[0], range[1]).groupBy("DATE(create_time)");
        }

        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);
        for (Map<String, Object> row : results) {
            int revenue = ((Number) row.get("revenue")).intValue();
            if (timeRange == TimeRangeEnum.TODAY) {
                int hour = ((Number) row.get("hour")).intValue();
                int index = hour / 2;
                if (index < data.size()) data.set(index, data.get(index) + revenue);
            } else if (timeRange == TimeRangeEnum.WEEK) {
                int weekday = ((Number) row.get("weekday")).intValue() - 1;
                if (weekday >= 0 && weekday < data.size()) data.set(weekday, revenue);
            }
        }

        return ChartVO.builder().key("revenueTrend").title("营收趋势").type("line")
            .data(LineChartDataVO.builder().xAxis(xAxis)
                .series(List.of(LineChartDataVO.SeriesVO.builder().name("营收(万元)").data(data).build())).build()).build();
    }

    private ChartVO buildPaymentStatusChart() {
        return ChartVO.builder().key("paymentStatus").title("回款状态").type("pie")
            .data(PieChartDataVO.builder().items(List.of(
                PieChartDataVO.ItemVO.builder().name("暂无数据").value(0).build()
            )).build()).build();
    }

    private ChartVO buildDeptRevenueChart(LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("operator_dept_name, IFNULL(SUM(estimated_cost), 0) / 10000 as revenue")
               .eq("status", 80).between("create_time", range[0], range[1])
               .isNotNull("operator_dept_name")
               .groupBy("operator_dept_name").orderByDesc("revenue").last("LIMIT 10");

        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);
        List<String> names = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (Map<String, Object> row : results) {
            names.add((String) row.get("operator_dept_name"));
            data.add(((Number) row.get("revenue")).intValue());
        }
        if (names.isEmpty()) {
            names.add("暂无数据");
            data.add(0);
        }

        return ChartVO.builder().key("deptRevenue").title("各部门业绩").type("bar")
            .data(LineChartDataVO.builder().xAxis(names)
                .series(List.of(LineChartDataVO.SeriesVO.builder().name("营收(万元)").data(data).build())).build()).build();
    }

    private DashboardVO buildEmptyDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setCards(new ArrayList<>());
        vo.setCharts(new ArrayList<>());
        vo.setTodos(new ArrayList<>());
        return vo;
    }
}
