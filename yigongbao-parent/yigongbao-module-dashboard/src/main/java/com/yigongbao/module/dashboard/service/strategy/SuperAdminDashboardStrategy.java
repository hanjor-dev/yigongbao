package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 超级管理员数据概览策略
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SuperAdminDashboardStrategy implements DashboardStrategy {

    private final OrderMainMapper orderMapper;
    private final ProductionProductMapper productMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建超级管理员数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = new DashboardVO();
        try {
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
            vo.setCards(buildCards(range));
            vo.setCharts(buildCharts(range, query));
            vo.setTodos(new ArrayList<>());
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<CardVO> buildCards(LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        Long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<OrderMainEntity>()
                .ne(OrderMainEntity::getStatus, FlowStatusEnum.CANCELLED.getValue())
                .between(OrderMainEntity::getCreateTime, range[0], range[1]));
        cards.add(CardVO.builder().key("totalOrders").title("订单总数").value(totalOrders).unit("单").build());

        Long ongoingOrders = orderMapper.selectCount(new LambdaQueryWrapper<OrderMainEntity>()
                .notIn(OrderMainEntity::getStatus,
                        FlowStatusEnum.WAREHOUSE_OUT.getValue(),
                        FlowStatusEnum.COMPLETED.getValue(),
                        FlowStatusEnum.CANCELLED.getValue())
                .between(OrderMainEntity::getCreateTime, range[0], range[1]));
        cards.add(CardVO.builder().key("ongoingOrders").title("进行中订单").value(ongoingOrders).unit("单").build());

        Long completedOrders = orderMapper.selectCount(new LambdaQueryWrapper<OrderMainEntity>()
                .in(OrderMainEntity::getStatus,
                        FlowStatusEnum.WAREHOUSE_OUT.getValue(),
                        FlowStatusEnum.COMPLETED.getValue())
                .between(OrderMainEntity::getActualCompleteTime, range[0], range[1]));
        cards.add(CardVO.builder().key("completedOrders").title("已完成订单").value(completedOrders).unit("单").build());

        QueryWrapper<OrderMainEntity> cycleWrapper = new QueryWrapper<>();
        cycleWrapper.select("IFNULL(AVG(TIMESTAMPDIFF(HOUR, create_time, update_time)), 0) as avg_hours")
                .in("status", FlowStatusEnum.COMPLETED.getValue(), FlowStatusEnum.WAREHOUSE_OUT.getValue())
                .between("create_time", range[0], range[1]);
        List<Map<String, Object>> cycleResults = orderMapper.selectMaps(cycleWrapper);
        Integer avgHours = 0;
        if (!cycleResults.isEmpty() && cycleResults.get(0) != null && cycleResults.get(0).get("avg_hours") != null) {
            avgHours = ((Number) cycleResults.get(0).get("avg_hours")).intValue();
        }
        cards.add(CardVO.builder().key("avgOrderCycle").title("平均订单周期").value(String.valueOf(avgHours)).unit("小时").build());

        return cards;
    }

    private List<ChartVO> buildCharts(LocalDateTime[] range, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildOrderTrendChart(range, query));
        charts.add(buildOrderPhaseDistributionChart(range));
        charts.add(buildTopDeptOrdersChart(range));
        charts.add(buildTopOrgOrdersChart(range));
        return charts;
    }

    private ChartVO buildOrderTrendChart(LocalDateTime[] range, DashboardQueryDTO query) {
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<Integer> newOrderData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> completedOrderData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        QueryWrapper<OrderMainEntity> newWrapper = new QueryWrapper<>();
        newWrapper.between("create_time", range[0], range[1]);
        addGroupByClause(newWrapper, query, range, "create_time");
        fillChartData(orderMapper.selectMaps(newWrapper), newOrderData, query, range);

        QueryWrapper<OrderMainEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.in("status", FlowStatusEnum.WAREHOUSE_OUT.getValue(), FlowStatusEnum.COMPLETED.getValue())
                .between("actual_complete_time", range[0], range[1]);
        addGroupByClause(completedWrapper, query, range, "actual_complete_time");
        fillChartData(orderMapper.selectMaps(completedWrapper), completedOrderData, query, range);

        return ChartVO.builder().key("orderTrend").title("订单趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(xAxis).series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("新增订单").data(newOrderData).build(),
                        LineChartDataVO.SeriesVO.builder().name("完成订单").data(completedOrderData).build()
                )).build()).build();
    }

    private ChartVO buildOrderPhaseDistributionChart(LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("phase, COUNT(*) as count")
                .between("create_time", range[0], range[1])
                .groupBy("phase");
        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);

        Map<Integer, Integer> phaseCountMap = new HashMap<>();
        for (Map<String, Object> row : results) {
            Integer phase = ((Number) row.get("phase")).intValue();
            int count = ((Number) row.get("count")).intValue();
            phaseCountMap.put(phase, count);
        }

        List<String> xAxis = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (FlowPhaseEnum phaseEnum : FlowPhaseEnum.values()) {
            xAxis.add(phaseEnum.getName());
            data.add(phaseCountMap.getOrDefault(phaseEnum.getValue(), 0));
        }

        return ChartVO.builder().key("orderPhaseDistribution").title("订单流程分布").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        BarChartDataVO.SeriesVO.builder().name("订单数").data(data).build()
                )).build()).build();
    }

    private ChartVO buildTopDeptOrdersChart(LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("operator_dept_name, COUNT(*) as count")
                .isNotNull("operator_dept_name")
                .between("create_time", range[0], range[1])
                .groupBy("operator_dept_name")
                .orderByDesc("count")
                .last("LIMIT 3");
        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);

        List<String> xAxis = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (Map<String, Object> row : results) {
            xAxis.add((String) row.get("operator_dept_name"));
            data.add(((Number) row.get("count")).intValue());
        }
        if (xAxis.isEmpty()) {
            xAxis.add("暂无数据");
            data.add(0);
        }

        return ChartVO.builder().key("topDeptOrders").title("业务部门Top3订单量").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        BarChartDataVO.SeriesVO.builder().name("订单数").data(data).build()
                )).build()).build();
    }

    private ChartVO buildTopOrgOrdersChart(LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("org_name, COUNT(*) as count")
                .isNotNull("org_name")
                .between("create_time", range[0], range[1])
                .groupBy("org_name")
                .orderByDesc("count")
                .last("LIMIT 5");
        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);

        List<String> xAxis = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        for (Map<String, Object> row : results) {
            xAxis.add((String) row.get("org_name"));
            data.add(((Number) row.get("count")).intValue());
        }
        if (xAxis.isEmpty()) {
            xAxis.add("暂无数据");
            data.add(0);
        }

        return ChartVO.builder().key("topOrgOrders").title("活跃机构Top5订单量").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        BarChartDataVO.SeriesVO.builder().name("订单数").data(data).build()
                )).build()).build();
    }

    private void addGroupByClause(QueryWrapper<OrderMainEntity> wrapper, DashboardQueryDTO query, LocalDateTime[] range, String timeField) {
        switch (query.getTimeRangeEnum()) {
            case TODAY: wrapper.select("HOUR(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case WEEK: wrapper.select("WEEKDAY(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case MONTH: wrapper.select("DAY(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case QUARTER:
            case YEAR: wrapper.select("MONTH(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case CUSTOM:
                long days = java.time.temporal.ChronoUnit.DAYS.between(range[0].toLocalDate(), range[1].toLocalDate()) + 1;
                if (days <= 1) {
                    wrapper.select("HOUR(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 7) {
                    wrapper.select("DATEDIFF(" + timeField + ", '" + range[0].toLocalDate() + "') as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 31) {
                    wrapper.select("DAY(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else {
                    wrapper.select("MONTH(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit");
                }
                break;
        }
    }

    private void fillChartData(List<Map<String, Object>> results, List<Integer> data, DashboardQueryDTO query, LocalDateTime[] range) {
        for (Map<String, Object> row : results) {
            Number timeUnitNum = (Number) row.get("time_unit");
            if (timeUnitNum == null) continue;
            int timeUnit = timeUnitNum.intValue();
            int count = ((Number) row.get("count")).intValue();
            int index = -1;
            switch (query.getTimeRangeEnum()) {
                case TODAY: index = timeUnit / 2; break;
                case WEEK: index = timeUnit; break;
                case MONTH: index = (timeUnit - 1) / 5; break;
                case QUARTER:
                case YEAR: index = timeUnit - range[0].getMonthValue(); break;
                case CUSTOM:
                    long days = java.time.temporal.ChronoUnit.DAYS.between(range[0].toLocalDate(), range[1].toLocalDate()) + 1;
                    if (days <= 1) {
                        index = timeUnit / 2;
                    } else if (days <= 7) {
                        index = timeUnit;
                    } else if (days <= 31) {
                        index = (timeUnit - 1) / 5;
                    } else {
                        index = timeUnit - range[0].getMonthValue();
                    }
                    break;
            }
            if (index >= 0 && index < data.size()) {
                data.set(index, data.get(index) + count);
            }
        }
    }

    private DashboardVO buildEmptyDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setCards(new ArrayList<>());
        vo.setCharts(new ArrayList<>());
        vo.setTodos(new ArrayList<>());
        return vo;
    }
}
