package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.dashboard.vo.BarChartDataVO.SeriesVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 生产员数据概览策略
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionWorkerDashboardStrategy implements DashboardStrategy {

    private final ProductionRecordMapper productionRecordMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建生产员数据概览: userId={}, query={}", userId, query);

        DashboardVO vo = new DashboardVO();
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);
        vo.setCards(buildCards(userId, range));
        vo.setCharts(buildCharts(userId, query));
        vo.setTodos(new ArrayList<>());
        log.info("生产员数据概览构建完成: userId={}", userId);
        return vo;
    }

    private List<CardVO> buildCards(Long userId, ProductionDashboardQueryHelper.Range range) {
        List<CardVO> cards = new ArrayList<>();

        // 我的任务（该生产员的所有生产流转卡）
        LambdaQueryWrapper<ProductionRecordEntity> myTasksWrapper = new LambdaQueryWrapper<>();
        myTasksWrapper.eq(ProductionRecordEntity::getProducerId, userId)
                .ge(ProductionRecordEntity::getCreateTime, range.startInclusive())
                .lt(ProductionRecordEntity::getCreateTime, range.endExclusive());
        Long myTasks = productionRecordMapper.selectCount(myTasksWrapper);

        cards.add(CardVO.builder()
                .key("myTasks")
                .title("我的任务")
                .value(myTasks)
                .unit("单")
                .build());

        // 生产中（当前状态为打印阶段3010-3090或后处理阶段4010-4090，实时积压）
        LambdaQueryWrapper<ProductionRecordEntity> inProductionWrapper = new LambdaQueryWrapper<>();
        inProductionWrapper.eq(ProductionRecordEntity::getProducerId, userId)
                .and(w -> w.between(ProductionRecordEntity::getStatus, 3010, 3090)
                        .or().between(ProductionRecordEntity::getStatus, 4010, 4090));
        Long inProduction = productionRecordMapper.selectCount(inProductionWrapper);

        cards.add(CardVO.builder()
                .key("inProduction")
                .title("生产中")
                .value(inProduction)
                .unit("单")
                .build());

        // 质检中（状态为质检阶段5010-5090，实时积压）
        LambdaQueryWrapper<ProductionRecordEntity> inQcWrapper = new LambdaQueryWrapper<>();
        inQcWrapper.eq(ProductionRecordEntity::getProducerId, userId)
                .between(ProductionRecordEntity::getStatus, 5010, 5090);
        Long inQc = productionRecordMapper.selectCount(inQcWrapper);

        cards.add(CardVO.builder()
                .key("inQc")
                .title("质检中")
                .value(inQc)
                .unit("单")
                .build());

        // 仓储中（状态为待入库6010或已入库6020，实时积压）
        LambdaQueryWrapper<ProductionRecordEntity> inWarehouseWrapper = new LambdaQueryWrapper<>();
        inWarehouseWrapper.eq(ProductionRecordEntity::getProducerId, userId)
                .in(ProductionRecordEntity::getStatus, 6010, 6020);
        Long inWarehouse = productionRecordMapper.selectCount(inWarehouseWrapper);

        cards.add(CardVO.builder()
                .key("inWarehouse")
                .title("仓储中")
                .value(inWarehouse)
                .unit("单")
                .build());

        // 已完成（状态为已出库6030或已完成8010，基于后处理结束时间）
        LambdaQueryWrapper<ProductionRecordEntity> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(ProductionRecordEntity::getProducerId, userId)
                .in(ProductionRecordEntity::getStatus, 6030, 8010)
                .isNotNull(ProductionRecordEntity::getPostProcessingEndTime)
                .ge(ProductionRecordEntity::getPostProcessingEndTime, range.startInclusive())
                .lt(ProductionRecordEntity::getPostProcessingEndTime, range.endExclusive());
        Long completed = productionRecordMapper.selectCount(completedWrapper);

        cards.add(CardVO.builder()
                .key("completed")
                .title("已完成")
                .value(completed)
                .unit("单")
                .build());

        return cards;
    }

    private List<ChartVO> buildCharts(Long userId, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildMonthComparisonChart(userId));
        charts.add(buildProductionTrendChart(userId, query));
        return charts;
    }

    private ChartVO buildMonthComparisonChart(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate thisMonthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        List<String> xAxis = List.of("第1周", "第2周", "第3周", "第4周", "第5周");
        List<Integer> thisMonthData = new ArrayList<>(Collections.nCopies(5, 0));
        List<Integer> lastMonthData = new ArrayList<>(Collections.nCopies(5, 0));

        // 本月数据（统计所有状态）
        QueryWrapper<ProductionRecordEntity> thisMonthWrapper = new QueryWrapper<>();
        thisMonthWrapper.select("FLOOR((DAY(create_time) - 1) / 7) + 1 as week_of_month, COUNT(*) as count")
                .eq("producer_id", userId)
                .ge("create_time", thisMonthStart.atStartOfDay())
                .lt("create_time", thisMonthStart.plusMonths(1).atStartOfDay())
                .groupBy("week_of_month");

        List<Map<String, Object>> thisMonthResults = productionRecordMapper.selectMaps(thisMonthWrapper);
        for (Map<String, Object> row : thisMonthResults) {
            int weekOfMonth = ((Number) row.get("week_of_month")).intValue();
            int count = ((Number) row.get("count")).intValue();
            if (weekOfMonth >= 1 && weekOfMonth <= 5) {
                thisMonthData.set(weekOfMonth - 1, count);
            }
        }

        // 上月数据（统计所有状态）
        QueryWrapper<ProductionRecordEntity> lastMonthWrapper = new QueryWrapper<>();
        lastMonthWrapper.select("FLOOR((DAY(create_time) - 1) / 7) + 1 as week_of_month, COUNT(*) as count")
                .eq("producer_id", userId)
                .ge("create_time", lastMonthStart.atStartOfDay())
                .lt("create_time", thisMonthStart.atStartOfDay())
                .groupBy("week_of_month");

        List<Map<String, Object>> lastMonthResults = productionRecordMapper.selectMaps(lastMonthWrapper);
        for (Map<String, Object> row : lastMonthResults) {
            int weekOfMonth = ((Number) row.get("week_of_month")).intValue();
            int count = ((Number) row.get("count")).intValue();
            if (weekOfMonth >= 1 && weekOfMonth <= 5) {
                lastMonthData.set(weekOfMonth - 1, count);
            }
        }

        return ChartVO.builder()
                .key("monthComparison")
                .title("环比生产对比")
                .type("line")
                .data(LineChartDataVO.builder()
                        .xAxis(xAxis)
                        .series(List.of(
                                LineChartDataVO.SeriesVO.builder().name("本月").data(thisMonthData).build(),
                                LineChartDataVO.SeriesVO.builder().name("上月").data(lastMonthData).build()
                        ))
                        .build())
                .build();
    }

    private ChartVO buildProductionTrendChart(Long userId, DashboardQueryDTO query) {
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());

        List<Integer> inProductionData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> completedData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> otherStageData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        // 生产中数据
        QueryWrapper<ProductionRecordEntity> inProductionWrapper = new QueryWrapper<>();
        inProductionWrapper.eq("producer_id", userId)
                .and(w -> w.between("status", 3010, 3090).or().between("status", 4010, 4090))
                .ge("create_time", range.startInclusive()).lt("create_time", range.endExclusive());
        addGroupByClause(inProductionWrapper, query, range, "create_time");

        List<Map<String, Object>> inProductionResults = productionRecordMapper.selectMaps(inProductionWrapper);
        fillChartData(inProductionResults, inProductionData, query, range);

        // 已完成数据（基于后处理结束时间）
        QueryWrapper<ProductionRecordEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.eq("producer_id", userId)
                .in("status", 6030, 8010)
                .isNotNull("post_processing_end_time")
                .ge("post_processing_end_time", range.startInclusive()).lt("post_processing_end_time", range.endExclusive());
        addGroupByClause(completedWrapper, query, range, "post_processing_end_time");

        List<Map<String, Object>> completedResults = productionRecordMapper.selectMaps(completedWrapper);
        fillChartData(completedResults, completedData, query, range);

        // 其他阶段数据（质检+仓储+取消）
        QueryWrapper<ProductionRecordEntity> otherStageWrapper = new QueryWrapper<>();
        otherStageWrapper.eq("producer_id", userId)
                .and(w -> w.between("status", 5010, 5090).or().between("status", 6010, 6020).or().eq("status", 9010))
                .ge("create_time", range.startInclusive()).lt("create_time", range.endExclusive());
        addGroupByClause(otherStageWrapper, query, range, "create_time");

        List<Map<String, Object>> otherStageResults = productionRecordMapper.selectMaps(otherStageWrapper);
        fillChartData(otherStageResults, otherStageData, query, range);

        return ChartVO.builder()
                .key("productionTrend")
                .title("生产趋势")
                .type("bar")
                .data(BarChartDataVO.builder()
                        .xAxis(xAxis)
                        .series(List.of(
                                SeriesVO.builder().name("生产中").data(inProductionData).build(),
                                SeriesVO.builder().name("其他阶段").data(otherStageData).build(),
                                SeriesVO.builder().name("已完成").data(completedData).build()
                        ))
                        .build())
                .build();
    }

    private void addGroupByClause(QueryWrapper<ProductionRecordEntity> wrapper, DashboardQueryDTO query,
                                  ProductionDashboardQueryHelper.Range range, String column) {
        wrapper.select(ProductionDashboardQueryHelper.groupSelect(query, range, column)
                + " as time_unit, COUNT(*) as count").groupBy("time_unit");
    }

    private void fillChartData(List<Map<String, Object>> results, List<Integer> data, DashboardQueryDTO query,
                               ProductionDashboardQueryHelper.Range range) {
        for (Map<String, Object> row : results) {
            int timeUnit = ((Number) row.get("time_unit")).intValue();
            int count = ((Number) row.get("count")).intValue();

            int index = ProductionDashboardQueryHelper.bucketIndex(query, range, timeUnit);
            if (index >= 0 && index < data.size()) {
                data.set(index, data.get(index) + count);
            }
        }
    }

}
