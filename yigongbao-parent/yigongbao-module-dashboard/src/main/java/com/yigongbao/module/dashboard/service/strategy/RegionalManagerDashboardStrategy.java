package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserManagedOrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegionalManagerDashboardStrategy implements DashboardStrategy {

    private final OrderMainMapper orderMapper;
    private final UserMapper userMapper;
    private final UserManagedOrgService userManagedOrgService;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建区域管理员数据概览: userId={}, query={}", userId, query);

        try {
            TimeRangeEnum timeRange = query.getTimeRangeEnum();
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(timeRange, query.getStartDate(), query.getEndDate());
            List<Long> effectiveOrgIds = userManagedOrgService.getEffectiveOrgIds(userId);
            if (effectiveOrgIds.isEmpty()) {
                return buildEmptyDashboard();
            }
            int salesmanCount = Math.toIntExact(userMapper.selectCount(
                    new QueryWrapper<UserEntity>().in("org_id", effectiveOrgIds).eq("status", 1)));

            DashboardVO vo = new DashboardVO();
            vo.setCards(buildCards(effectiveOrgIds, range, salesmanCount));
            vo.setCharts(buildCharts(effectiveOrgIds, range, query));
            vo.setTodos(new ArrayList<>());
            return vo;
        } catch (Exception e) {
            log.error("构建区域管理员数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
    }

    private List<CardVO> buildCards(List<Long> orgIds, LocalDateTime[] range, int userCount) {
        long total = orderMapper.selectCount(new QueryWrapper<OrderMainEntity>()
            .in("org_id", orgIds)
            .between("create_time", range[0], range[1]));

            long pendingAudit = orderMapper.selectCount(new QueryWrapper<OrderMainEntity>()
                .in("org_id", orgIds)
                .eq("status", FlowStatusEnum.PENDING_DATA_AUDIT.getValue())
                .between("create_time", range[0], range[1]));

        long inProgress = orderMapper.selectCount(new QueryWrapper<OrderMainEntity>()
            .in("org_id", orgIds)
            .notIn("status", 6030, 8010, 9010)
            .between("create_time", range[0], range[1]));

        long completed = orderMapper.selectCount(new QueryWrapper<OrderMainEntity>()
            .in("org_id", orgIds)
            .in("status", List.of(6030, 8010))
            .between("create_time", range[0], range[1]));

        double avgOrders = userCount > 0 ? (double) total / userCount : 0;

        return List.of(
            CardVO.builder().key("managedOrgOrders").title("管理机构订单").value(total).unit("单").build(),
            CardVO.builder().key("pendingAudit").title("待设计审核订单").value(pendingAudit).unit("单").build(),
            CardVO.builder().key("inProgress").title("进行中订单").value(inProgress).unit("单").build(),
            CardVO.builder().key("completedOrders").title("已完成").value(completed).unit("单").build(),
            CardVO.builder().key("avgOrders").title("人均订单数").value(String.format("%.1f", avgOrders)).unit("单").build()
        );
    }

    private List<ChartVO> buildCharts(List<Long> orgIds, LocalDateTime[] range, DashboardQueryDTO query) {
        return List.of(
            buildOrderTrendChart(orgIds, range, query),
            buildSalesmanTop10Chart(orgIds, range),
            buildDailySubmissionsChart(orgIds, range, query)
        );
    }

    private ChartVO buildOrderTrendChart(List<Long> orgIds, LocalDateTime[] range, DashboardQueryDTO query) {
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

        QueryWrapper<OrderMainEntity> newWrapper = new QueryWrapper<>();
        newWrapper.in("org_id", orgIds).between("create_time", range[0], range[1]);
        switch (effectiveRange) {
            case TODAY:
                newWrapper.select("HOUR(create_time) as hour, COUNT(*) as count").groupBy("HOUR(create_time)");
                break;
            case WEEK:
                newWrapper.select("DAYOFWEEK(create_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(create_time)");
                break;
            case MONTH:
                newWrapper.select("DAY(create_time) as day, COUNT(*) as count").groupBy("DAY(create_time)");
                break;
            case QUARTER:
            case YEAR:
                newWrapper.select("MONTH(create_time) as month, COUNT(*) as count").groupBy("MONTH(create_time)");
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

        QueryWrapper<OrderMainEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.in("org_id", orgIds).in("status", List.of(6030, 8010))
                        .between("update_time", range[0], range[1]);
        switch (effectiveRange) {
            case TODAY:
                completedWrapper.select("HOUR(update_time) as hour, COUNT(*) as count").groupBy("HOUR(update_time)");
                break;
            case WEEK:
                completedWrapper.select("DAYOFWEEK(update_time) as weekday, COUNT(*) as count").groupBy("DAYOFWEEK(update_time)");
                break;
            case MONTH:
                completedWrapper.select("DAY(update_time) as day, COUNT(*) as count").groupBy("DAY(update_time)");
                break;
            case QUARTER:
            case YEAR:
                completedWrapper.select("MONTH(update_time) as month, COUNT(*) as count").groupBy("MONTH(update_time)");
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
            .key("orderTrend").title("管理机构订单趋势").type("line")
            .data(LineChartDataVO.builder()
                .xAxis(xAxis)
                .series(List.of(
                    LineChartDataVO.SeriesVO.builder().name("新提订单").data(newOrders).build(),
                    LineChartDataVO.SeriesVO.builder().name("完成订单").data(completedOrders).build()
                ))
                .build())
            .build();
    }

    private ChartVO buildSalesmanTop10Chart(List<Long> orgIds, LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("operator_id, operator_name, " +
                "SUM(CASE WHEN status NOT IN (6030, 8010, 9010) THEN 1 ELSE 0 END) as in_progress, " +
                "SUM(CASE WHEN status IN (6030, 8010) THEN 1 ELSE 0 END) as completed")
               .in("org_id", orgIds)
               .between("create_time", range[0], range[1])
               .groupBy("operator_id")
               .orderByDesc("in_progress + completed")
               .last("LIMIT 10");

        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);
        List<String> names = new ArrayList<>();
        List<Integer> inProgressData = new ArrayList<>();
        List<Integer> completedData = new ArrayList<>();

        for (Map<String, Object> row : results) {
            names.add((String) row.get("operator_name"));
            inProgressData.add(((Number) row.get("in_progress")).intValue());
            completedData.add(((Number) row.get("completed")).intValue());
        }

        if (names.isEmpty()) {
            names.add("暂无数据");
            inProgressData.add(0);
            completedData.add(0);
        }

        return ChartVO.builder()
            .key("salesmanTop10").title("业务员TOP10").type("bar")
            .data(LineChartDataVO.builder().xAxis(names).series(List.of(
                LineChartDataVO.SeriesVO.builder().name("进行中").data(inProgressData).build(),
                LineChartDataVO.SeriesVO.builder().name("已完成").data(completedData).build()
            )).build())
            .build();
    }

    private ChartVO buildDailySubmissionsChart(List<Long> orgIds, LocalDateTime[] range, DashboardQueryDTO query) {
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
        wrapper.in("org_id", orgIds).between("create_time", range[0], range[1]);

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
            .key("dailySubmissions").title("提单时间分布").type("bar")
            .data(LineChartDataVO.builder()
                .xAxis(xAxis)
                .series(List.of(LineChartDataVO.SeriesVO.builder().name("提单数").data(data).build()))
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
