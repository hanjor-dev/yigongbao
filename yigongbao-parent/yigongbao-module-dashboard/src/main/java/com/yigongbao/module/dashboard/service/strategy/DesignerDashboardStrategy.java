package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.dashboard.vo.LineChartDataVO.SeriesVO;
import com.yigongbao.module.dashboard.vo.PieChartDataVO.ItemVO;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设计师数据概览策略
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DesignerDashboardStrategy implements DashboardStrategy {

    private final OrderMainMapper orderMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建设计师数据概览: userId={}, query={}", userId, query);

        DashboardVO vo = new DashboardVO();
        try {
            TimeRangeEnum timeRange = query.getTimeRangeEnum();
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(timeRange, query.getStartDate(), query.getEndDate());

            vo.setCards(buildCards(userId, range));
            vo.setCharts(buildCharts(userId, range, query));
            vo.setTodos(new ArrayList<>());

            log.info("设计师数据概览构建完成: userId={}", userId);
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<ChartVO> buildCharts(Long userId, LocalDateTime[] range, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildWorkorderTrendChart(userId, range, query));
        charts.add(buildWorkorderPhaseChart(userId, range));
        return charts;
    }

    private ChartVO buildWorkorderTrendChart(Long userId, LocalDateTime[] range, DashboardQueryDTO query) {
        TimeRangeEnum timeRange = query.getTimeRangeEnum();
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(timeRange, query.getStartDate(), query.getEndDate());
        List<Integer> newOrders = new ArrayList<>();
        List<Integer> completedOrders = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) {
            newOrders.add(0);
            completedOrders.add(0);
        }

        TimeRangeEnum effectiveRange = timeRange == TimeRangeEnum.CUSTOM
            ? TimeRangeUtil.getEffectiveTimeRange(query.getStartDate(), query.getEndDate())
            : timeRange;

        // 新接订单（design_start_time）
        QueryWrapper<OrderMainEntity> newWrapper = new QueryWrapper<>();
        newWrapper.eq("designer_id", userId).isNotNull("design_start_time")
                  .between("design_start_time", range[0], range[1]);
        switch (effectiveRange) {
            case TODAY:
                newWrapper.select("HOUR(design_start_time) as hour, COUNT(*) as count").groupBy("HOUR(design_start_time)");
                break;
            case WEEK:
                newWrapper.select("DAYOFWEEK(design_start_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(design_start_time)");
                break;
            case MONTH:
                newWrapper.select("DAY(design_start_time) as day, COUNT(*) as count").groupBy("DAY(design_start_time)");
                break;
            case QUARTER:
            case YEAR:
                newWrapper.select("MONTH(design_start_time) as month, COUNT(*) as count").groupBy("MONTH(design_start_time)");
                break;
        }

        List<Map<String, Object>> newResults = orderMapper.selectMaps(newWrapper);
        for (Map<String, Object> row : newResults) {
            int count = ((Number) row.get("count")).intValue();
            if (effectiveRange == TimeRangeEnum.TODAY) {
                int hour = ((Number) row.get("hour")).intValue();
                int index = hour / 2;
                if (index < newOrders.size()) newOrders.set(index, newOrders.get(index) + count);
            } else if (effectiveRange == TimeRangeEnum.WEEK) {
                int weekday = ((Number) row.get("weekday")).intValue() - 1;
                if (weekday >= 0 && weekday < newOrders.size()) newOrders.set(weekday, count);
            } else if (effectiveRange == TimeRangeEnum.MONTH) {
                int day = ((Number) row.get("day")).intValue();
                int index = (day - 1) / 5;
                if (index < newOrders.size()) newOrders.set(index, newOrders.get(index) + count);
            } else {
                int month = ((Number) row.get("month")).intValue();
                int startMonth = range[0].getMonthValue();
                int index = month - startMonth;
                if (index >= 0 && index < newOrders.size()) newOrders.set(index, newOrders.get(index) + count);
            }
        }

        // 完成设计订单（design_submit_time + 状态>=2030）
        QueryWrapper<OrderMainEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.eq("designer_id", userId).ge("status", 2030).isNotNull("design_submit_time")
                        .between("design_submit_time", range[0], range[1]);
        switch (effectiveRange) {
            case TODAY:
                completedWrapper.select("HOUR(design_submit_time) as hour, COUNT(*) as count").groupBy("HOUR(design_submit_time)");
                break;
            case WEEK:
                completedWrapper.select("DAYOFWEEK(design_submit_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(design_submit_time)");
                break;
            case MONTH:
                completedWrapper.select("DAY(design_submit_time) as day, COUNT(*) as count").groupBy("DAY(design_submit_time)");
                break;
            case QUARTER:
            case YEAR:
                completedWrapper.select("MONTH(design_submit_time) as month, COUNT(*) as count").groupBy("MONTH(design_submit_time)");
                break;
        }

        List<Map<String, Object>> completedResults = orderMapper.selectMaps(completedWrapper);
        for (Map<String, Object> row : completedResults) {
            int count = ((Number) row.get("count")).intValue();
            if (effectiveRange == TimeRangeEnum.TODAY) {
                int hour = ((Number) row.get("hour")).intValue();
                int index = hour / 2;
                if (index < completedOrders.size()) completedOrders.set(index, completedOrders.get(index) + count);
            } else if (effectiveRange == TimeRangeEnum.WEEK) {
                int weekday = ((Number) row.get("weekday")).intValue() - 1;
                if (weekday >= 0 && weekday < completedOrders.size()) completedOrders.set(weekday, count);
            } else if (effectiveRange == TimeRangeEnum.MONTH) {
                int day = ((Number) row.get("day")).intValue();
                int index = (day - 1) / 5;
                if (index < completedOrders.size()) completedOrders.set(index, completedOrders.get(index) + count);
            } else {
                int month = ((Number) row.get("month")).intValue();
                int startMonth = range[0].getMonthValue();
                int index = month - startMonth;
                if (index >= 0 && index < completedOrders.size()) completedOrders.set(index, completedOrders.get(index) + count);
            }
        }

        return ChartVO.builder()
                .key("workorderTrend")
                .title("工单趋势")
                .type("line")
                .data(LineChartDataVO.builder()
                        .xAxis(xAxis)
                        .series(List.of(
                                SeriesVO.builder().name("新接订单").data(newOrders).build(),
                                SeriesVO.builder().name("完成设计").data(completedOrders).build()
                        ))
                        .build())
                .build();
    }

    private List<CardVO> buildCards(Long userId, LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        // 我的工单总数（全部阶段）
        LambdaQueryWrapper<OrderMainEntity> myWorkordersWrapper = new LambdaQueryWrapper<>();
        myWorkordersWrapper.eq(OrderMainEntity::getDesignerId, userId)
                           .between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long myWorkorders = orderMapper.selectCount(myWorkordersWrapper);

        cards.add(CardVO.builder()
                .key("myWorkorders")
                .title("我的工单")
                .value(myWorkorders)
                .unit("单")
                .link("/workorder")
                .build());

        // 待开始工单（状态=待设计 2010）
        LambdaQueryWrapper<OrderMainEntity> pendingStartWrapper = new LambdaQueryWrapper<>();
        pendingStartWrapper.eq(OrderMainEntity::getDesignerId, userId)
                            .eq(OrderMainEntity::getStatus, 2010);
        Long pendingStart = orderMapper.selectCount(pendingStartWrapper);

        cards.add(CardVO.builder()
                .key("pendingStart")
                .title("待开始工单")
                .value(pendingStart)
                .unit("单")
                .link("/workorder?status=pending_start")
                .build());

        // 设计中工单（状态=设计中 2020）
        LambdaQueryWrapper<OrderMainEntity> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(OrderMainEntity::getDesignerId, userId)
                     .eq(OrderMainEntity::getStatus, 2020);
        Long inProgress = orderMapper.selectCount(inProgressWrapper);

        cards.add(CardVO.builder()
                .key("inProgress")
                .title("设计中工单")
                .value(inProgress)
                .unit("单")
                .link("/workorder?status=in_progress")
                .build());

        // 已完成工单（设计完成 2030 及后续状态）
        LambdaQueryWrapper<OrderMainEntity> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(OrderMainEntity::getDesignerId, userId)
                        .ge(OrderMainEntity::getStatus, 2030)
                        .notIn(OrderMainEntity::getStatus, 9010)
                        .between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long completed = orderMapper.selectCount(completedWrapper);

        cards.add(CardVO.builder()
                .key("completedWorkorders")
                .title("已完成")
                .value(completed)
                .unit("单")
                .link("/workorder?status=completed")
                .build());

        return cards;
    }

    private List<ChartVO> buildCharts(Long userId, LocalDateTime[] range, TimeRangeEnum timeRange) {
        List<ChartVO> charts = new ArrayList<>();

        // 工单趋势（双折线：新接订单 + 完成设计订单）
        charts.add(buildWorkorderTrendChart(userId, range, timeRange));

        // 工单状态分布（按阶段分组）
        charts.add(buildWorkorderPhaseChart(userId, range));

        return charts;
    }

    private ChartVO buildWorkorderTrendChart(Long userId, LocalDateTime[] range, TimeRangeEnum timeRange) {
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(timeRange);
        List<Integer> newOrders = new ArrayList<>();
        List<Integer> completedOrders = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) {
            newOrders.add(0);
            completedOrders.add(0);
        }

        // 新接订单（design_start_time）
        QueryWrapper<OrderMainEntity> newWrapper = new QueryWrapper<>();
        newWrapper.eq("designer_id", userId).isNotNull("design_start_time")
                  .between("design_start_time", range[0], range[1]);
        switch (timeRange) {
            case TODAY:
                newWrapper.select("HOUR(design_start_time) as hour, COUNT(*) as count").groupBy("HOUR(design_start_time)");
                break;
            case WEEK:
                newWrapper.select("DAYOFWEEK(design_start_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(design_start_time)");
                break;
            case MONTH:
                newWrapper.select("DAY(design_start_time) as day, COUNT(*) as count").groupBy("DAY(design_start_time)");
                break;
            case QUARTER:
            case YEAR:
                newWrapper.select("MONTH(design_start_time) as month, COUNT(*) as count").groupBy("MONTH(design_start_time)");
                break;
        }

        List<Map<String, Object>> newResults = orderMapper.selectMaps(newWrapper);
        for (Map<String, Object> row : newResults) {
            int count = ((Number) row.get("count")).intValue();
            if (timeRange == TimeRangeEnum.TODAY) {
                int hour = ((Number) row.get("hour")).intValue();
                int index = hour / 2;
                if (index < newOrders.size()) newOrders.set(index, newOrders.get(index) + count);
            } else if (timeRange == TimeRangeEnum.WEEK) {
                int weekday = ((Number) row.get("weekday")).intValue() - 1;
                if (weekday >= 0 && weekday < newOrders.size()) newOrders.set(weekday, count);
            } else if (timeRange == TimeRangeEnum.MONTH) {
                int day = ((Number) row.get("day")).intValue();
                int index = (day - 1) / 5;
                if (index < newOrders.size()) newOrders.set(index, newOrders.get(index) + count);
            } else {
                int month = ((Number) row.get("month")).intValue();
                int startMonth = range[0].getMonthValue();
                int index = month - startMonth;
                if (index >= 0 && index < newOrders.size()) newOrders.set(index, newOrders.get(index) + count);
            }
        }

        // 完成设计订单（design_submit_time + 状态>=2030）
        QueryWrapper<OrderMainEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.eq("designer_id", userId).ge("status", 2030).isNotNull("design_submit_time")
                        .between("design_submit_time", range[0], range[1]);
        switch (timeRange) {
            case TODAY:
                completedWrapper.select("HOUR(design_submit_time) as hour, COUNT(*) as count").groupBy("HOUR(design_submit_time)");
                break;
            case WEEK:
                completedWrapper.select("DAYOFWEEK(design_submit_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(design_submit_time)");
                break;
            case MONTH:
                completedWrapper.select("DAY(design_submit_time) as day, COUNT(*) as count").groupBy("DAY(design_submit_time)");
                break;
            case QUARTER:
            case YEAR:
                completedWrapper.select("MONTH(design_submit_time) as month, COUNT(*) as count").groupBy("MONTH(design_submit_time)");
                break;
        }

        List<Map<String, Object>> completedResults = orderMapper.selectMaps(completedWrapper);
        for (Map<String, Object> row : completedResults) {
            int count = ((Number) row.get("count")).intValue();
            if (timeRange == TimeRangeEnum.TODAY) {
                int hour = ((Number) row.get("hour")).intValue();
                int index = hour / 2;
                if (index < completedOrders.size()) completedOrders.set(index, completedOrders.get(index) + count);
            } else if (timeRange == TimeRangeEnum.WEEK) {
                int weekday = ((Number) row.get("weekday")).intValue() - 1;
                if (weekday >= 0 && weekday < completedOrders.size()) completedOrders.set(weekday, count);
            } else if (timeRange == TimeRangeEnum.MONTH) {
                int day = ((Number) row.get("day")).intValue();
                int index = (day - 1) / 5;
                if (index < completedOrders.size()) completedOrders.set(index, completedOrders.get(index) + count);
            } else {
                int month = ((Number) row.get("month")).intValue();
                int startMonth = range[0].getMonthValue();
                int index = month - startMonth;
                if (index >= 0 && index < completedOrders.size()) completedOrders.set(index, completedOrders.get(index) + count);
            }
        }

        LineChartDataVO chartData = LineChartDataVO.builder()
                .xAxis(xAxis)
                .series(List.of(
                        SeriesVO.builder().name("新接订单").data(newOrders).build(),
                        SeriesVO.builder().name("完成设计").data(completedOrders).build()
                ))
                .build();

        return ChartVO.builder()
                .key("workorderTrend")
                .title("工单趋势")
                .type("line")
                .data(chartData)
                .build();
    }

    private ChartVO buildWorkorderPhaseChart(Long userId, LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("phase, COUNT(*) as count")
               .eq("designer_id", userId)
               .between("create_time", range[0], range[1])
               .groupBy("phase");

        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);
        List<ItemVO> items = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Integer phase = (Integer) row.get("phase");
            Integer count = ((Number) row.get("count")).intValue();
            String phaseName = FlowPhaseEnum.getByValue(phase) != null ?
                FlowPhaseEnum.getByValue(phase).getName() : "未知阶段";
            items.add(ItemVO.builder().name(phaseName).value(count).build());
        }

        if (items.isEmpty()) {
            items.add(ItemVO.builder().name("暂无数据").value(0).build());
        }

        PieChartDataVO chartData = PieChartDataVO.builder().items(items).build();

        return ChartVO.builder()
                .key("workorderPhase")
                .title("工单阶段分布")
                .type("pie")
                .data(chartData)
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
