package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.dashboard.vo.BarChartDataVO.SeriesVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionManagerDashboardStrategy implements DashboardStrategy {
    private final ProductionRecordMapper productionRecordMapper;
    private final UserMapper userMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建生产管理员数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = new DashboardVO();
        try {
            UserEntity currentUser = userMapper.selectById(userId);
            if (currentUser == null || currentUser.getCenterId() == null) {
                log.warn("生产管理员未绑定加工中心: userId={}", userId);
                return buildEmptyDashboard();
            }
            Long centerId = currentUser.getCenterId();
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
            vo.setCards(buildCards(centerId, range));
            vo.setCharts(buildCharts(centerId, query));
            vo.setTodos(new ArrayList<>());
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<CardVO> buildCards(Long centerId, LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();
        LambdaQueryWrapper<ProductionRecordEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .between(ProductionRecordEntity::getCreateTime, range[0], range[1]);
        Long total = productionRecordMapper.selectCount(totalWrapper);
        cards.add(CardVO.builder().key("totalProduction").title("生产单总数").value(total).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> inProductionWrapper = new LambdaQueryWrapper<>();
        inProductionWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .and(w -> w.between(ProductionRecordEntity::getStatus, 3010, 3090)
                        .or().between(ProductionRecordEntity::getStatus, 4010, 4090))
                .between(ProductionRecordEntity::getCreateTime, range[0], range[1]);
        Long inProduction = productionRecordMapper.selectCount(inProductionWrapper);
        cards.add(CardVO.builder().key("inProduction").title("生产中").value(inProduction).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> inQcWrapper = new LambdaQueryWrapper<>();
        inQcWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .between(ProductionRecordEntity::getStatus, 5010, 5090)
                .between(ProductionRecordEntity::getCreateTime, range[0], range[1]);
        Long inQc = productionRecordMapper.selectCount(inQcWrapper);
        cards.add(CardVO.builder().key("inQc").title("质检中").value(inQc).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> inWarehouseWrapper = new LambdaQueryWrapper<>();
        inWarehouseWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .in(ProductionRecordEntity::getStatus, 6010, 6020)
                .between(ProductionRecordEntity::getCreateTime, range[0], range[1]);
        Long inWarehouse = productionRecordMapper.selectCount(inWarehouseWrapper);
        cards.add(CardVO.builder().key("inWarehouse").title("仓储中").value(inWarehouse).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .in(ProductionRecordEntity::getStatus, 6030, 8010)
                .isNotNull(ProductionRecordEntity::getPostProcessingEndTime)
                .between(ProductionRecordEntity::getPostProcessingEndTime, range[0], range[1]);
        Long completed = productionRecordMapper.selectCount(completedWrapper);
        cards.add(CardVO.builder().key("completed").title("已完成").value(completed).unit("单").build());
        return cards;
    }

    private List<ChartVO> buildCharts(Long centerId, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildProductionTrendChart(centerId, query));
        charts.add(buildWorkerWorkloadChart(centerId, query));
        return charts;
    }

    private ChartVO buildProductionTrendChart(Long centerId, DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<Integer> inProductionData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> completedData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> otherStageData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        QueryWrapper<ProductionRecordEntity> inProductionWrapper = new QueryWrapper<>();
        inProductionWrapper.eq("processing_center_id", centerId)
                .and(w -> w.between("status", 3010, 3090).or().between("status", 4010, 4090))
                .between("create_time", range[0], range[1]);
        addGroupByClause(inProductionWrapper, query, range);
        fillChartData(productionRecordMapper.selectMaps(inProductionWrapper), inProductionData, query, range);

        QueryWrapper<ProductionRecordEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.eq("processing_center_id", centerId).in("status", 6030, 8010)
                .isNotNull("post_processing_end_time")
                .between("post_processing_end_time", range[0], range[1]);
        addGroupByClauseForCompletedTime(completedWrapper, query, range);
        fillChartData(productionRecordMapper.selectMaps(completedWrapper), completedData, query, range);

        QueryWrapper<ProductionRecordEntity> otherStageWrapper = new QueryWrapper<>();
        otherStageWrapper.eq("processing_center_id", centerId)
                .and(w -> w.between("status", 5010, 5090).or().between("status", 6010, 6020).or().eq("status", 9010))
                .between("create_time", range[0], range[1]);
        addGroupByClause(otherStageWrapper, query, range);
        fillChartData(productionRecordMapper.selectMaps(otherStageWrapper), otherStageData, query, range);

        return ChartVO.builder().key("productionTrend").title("生产趋势").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        SeriesVO.builder().name("生产中").data(inProductionData).build(),
                        SeriesVO.builder().name("其他阶段").data(otherStageData).build(),
                        SeriesVO.builder().name("已完成").data(completedData).build())).build()).build();
    }

    private ChartVO buildWorkerWorkloadChart(Long centerId, DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        QueryWrapper<ProductionRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.select("producer_id, producer_name, " +
                "SUM(CASE WHEN status BETWEEN 3010 AND 3090 OR status BETWEEN 4010 AND 4090 THEN 1 ELSE 0 END) as in_production, " +
                "SUM(CASE WHEN status IN (6030, 8010) THEN 1 ELSE 0 END) as completed")
                .eq("processing_center_id", centerId).isNotNull("producer_id")
                .between("create_time", range[0], range[1]).groupBy("producer_id, producer_name");

        List<Map<String, Object>> results = productionRecordMapper.selectMaps(wrapper);
        List<String> xAxis = new ArrayList<>();
        List<Integer> inProductionData = new ArrayList<>();
        List<Integer> completedData = new ArrayList<>();

        for (Map<String, Object> row : results) {
            xAxis.add((String) row.getOrDefault("producer_name", "未知"));
            inProductionData.add(((Number) row.get("in_production")).intValue());
            completedData.add(((Number) row.get("completed")).intValue());
        }
        if (xAxis.isEmpty()) {
            xAxis.add("暂无数据");
            inProductionData.add(0);
            completedData.add(0);
        }
        return ChartVO.builder().key("workerWorkload").title("生产员工作量").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        SeriesVO.builder().name("生产中").data(inProductionData).build(),
                        SeriesVO.builder().name("已完成").data(completedData).build())).build()).build();
    }

    private void addGroupByClause(QueryWrapper<ProductionRecordEntity> wrapper, DashboardQueryDTO query, LocalDateTime[] range) {
        switch (query.getTimeRangeEnum()) {
            case TODAY: wrapper.select("HOUR(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case WEEK: wrapper.select("DAYOFWEEK(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case MONTH: wrapper.select("DAY(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case QUARTER:
            case YEAR: wrapper.select("MONTH(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case CUSTOM:
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    range[0].toLocalDate(), range[1].toLocalDate()) + 1;
                if (days <= 1) {
                    wrapper.select("HOUR(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 7) {
                    wrapper.select("DATEDIFF(create_time, '" + range[0].toLocalDate() + "') as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 31) {
                    wrapper.select("DAY(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else {
                    wrapper.select("MONTH(create_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                }
                break;
        }
    }

    /** 为已完成数据添加分组子句（基于后处理结束时间） */
    private void addGroupByClauseForCompletedTime(QueryWrapper<ProductionRecordEntity> wrapper, DashboardQueryDTO query, LocalDateTime[] range) {
        switch (query.getTimeRangeEnum()) {
            case TODAY: wrapper.select("HOUR(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case WEEK: wrapper.select("DAYOFWEEK(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case MONTH: wrapper.select("DAY(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case QUARTER:
            case YEAR: wrapper.select("MONTH(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case CUSTOM:
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    range[0].toLocalDate(), range[1].toLocalDate()) + 1;
                if (days <= 1) {
                    wrapper.select("HOUR(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 7) {
                    wrapper.select("DATEDIFF(post_processing_end_time, '" + range[0].toLocalDate() + "') as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 31) {
                    wrapper.select("DAY(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else {
                    wrapper.select("MONTH(post_processing_end_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                }
                break;
        }
    }

    private void fillChartData(List<Map<String, Object>> results, List<Integer> data, DashboardQueryDTO query, LocalDateTime[] range) {
        for (Map<String, Object> row : results) {
            int timeUnit = ((Number) row.get("time_unit")).intValue();
            int count = ((Number) row.get("count")).intValue();
            int index = -1;
            switch (query.getTimeRangeEnum()) {
                case TODAY: index = timeUnit / 2; break;
                case WEEK: index = timeUnit - 1; break;
                case MONTH: index = (timeUnit - 1) / 5; break;
                case QUARTER:
                case YEAR: index = timeUnit - range[0].getMonthValue(); break;
                case CUSTOM:
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                        range[0].toLocalDate(), range[1].toLocalDate()) + 1;
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
