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
 * 业务员数据概览策略
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesmanDashboardStrategy implements DashboardStrategy {

    private final OrderMainMapper orderMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建业务员数据概览: userId=, query={}", userId, query);

        try {
            TimeRangeEnum timeRange = query.getTimeRangeEnum();
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(timeRange, query.getStartDate(), query.getEndDate());

            DashboardVO vo = new DashboardVO();
            vo.setCards(buildCards(userId, range));
            vo.setCharts(buildCharts(userId, range, timeRange, query.getStartDate(), query.getEndDate()));
            vo.setTodos(new ArrayList<>());

            log.info("业务员数据概览构建完成: userId={}", userId);
            return vo;
        } catch (Exception e) {
            log.error("构建业务员数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
    }

    private DashboardVO buildEmptyDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setCards(new ArrayList<>());
        vo.setCharts(new ArrayList<>());
        vo.setTodos(new ArrayList<>());
        return vo;
    }

    private List<CardVO> buildCards(Long userId, LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        // 我的订单总数
        LambdaQueryWrapper<OrderMainEntity> myOrdersWrapper = new LambdaQueryWrapper<>();
        myOrdersWrapper.eq(OrderMainEntity::getOperatorId, userId)
                       .between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long myOrders = orderMapper.selectCount(myOrdersWrapper);

        cards.add(CardVO.builder()
                .key("myOrders")
                .title("我的订单")
                .value(myOrders)
                .unit("单")
                .link("/order")
                .build());

        // 待处理订单（修改申请被驳回 + 数据审核未通过）
        LambdaQueryWrapper<OrderMainEntity> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(OrderMainEntity::getOperatorId, userId)
                      .in(OrderMainEntity::getStatus, 1040)
                      .between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long pending = orderMapper.selectCount(pendingWrapper);

        cards.add(CardVO.builder()
                .key("pendingOrders")
                .title("待处理订单")
                .value(pending)
                .unit("单")
                .link("/order?status=pending")
                .build());

        // 进行中订单（除已出库/已完成/已取消外的状态）
        LambdaQueryWrapper<OrderMainEntity> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(OrderMainEntity::getOperatorId, userId)
                        .notIn(OrderMainEntity::getStatus, 6030, 8010, 9010)
                        .between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long inProgress = orderMapper.selectCount(inProgressWrapper);

        cards.add(CardVO.builder()
                .key("inProgressOrders")
                .title("进行中订单")
                .value(inProgress)
                .unit("单")
                .link("/order?status=in_progress")
                .build());

        // 已完成订单（已出库或已完成）
        LambdaQueryWrapper<OrderMainEntity> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(OrderMainEntity::getOperatorId, userId)
                        .in(OrderMainEntity::getStatus, 6030, 8010)
                        .between(OrderMainEntity::getCreateTime, range[0], range[1]);
        Long completed = orderMapper.selectCount(completedWrapper);

        cards.add(CardVO.builder()
                .key("completedOrders")
                .title("已完成订单")
                .value(completed)
                .unit("单")
                .link("/order?status=completed")
                .build());

        return cards;
    }

    private List<ChartVO> buildCharts(Long userId, LocalDateTime[] range, TimeRangeEnum timeRange, LocalDate startDate, LocalDate endDate) {
        List<ChartVO> charts = new ArrayList<>();

        // 订单趋势
        charts.add(buildOrderTrendChart(userId, range, timeRange, startDate, endDate));

        // 订单阶段分布
        charts.add(buildOrderPhaseChart(userId, range));

        return charts;
    }

    private ChartVO buildOrderTrendChart(Long userId, LocalDateTime[] range, TimeRangeEnum timeRange, LocalDate startDate, LocalDate endDate) {
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(timeRange, startDate, endDate);
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < xAxis.size(); i++) {
            data.add(0);
        }

        TimeRangeEnum effectiveRange = timeRange == TimeRangeEnum.CUSTOM
            ? TimeRangeUtil.getEffectiveTimeRange(startDate, endDate)
            : timeRange;

        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("operator_id", userId).between("create_time", range[0], range[1]);

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

        LineChartDataVO chartData = LineChartDataVO.builder()
                .xAxis(xAxis)
                .series(List.of(SeriesVO.builder()
                        .name("订单数")
                        .data(data)
                        .build()))
                .build();

        return ChartVO.builder()
                .key("orderTrend")
                .title("订单趋势")
                .type("line")
                .data(chartData)
                .build();
    }

    private ChartVO buildOrderPhaseChart(Long userId, LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("phase, COUNT(*) as count")
               .eq("operator_id", userId)
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
                .key("orderPhase")
                .title("订单阶段分布")
                .type("pie")
                .data(chartData)
                .build();
    }
}
