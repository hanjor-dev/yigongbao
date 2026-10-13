package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.dashboard.vo.BarChartDataVO.SeriesVO;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionManagerDashboardStrategy implements DashboardStrategy {
    private final ProductionRecordMapper productionRecordMapper;
    private final UserMapper userMapper;
    private final ProcessingCenterMapper processingCenterMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建生产管理员数据概览: userId={}, query={}", userId, query);
        UserEntity currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        Long centerId = currentUser.getCenterId();
        if (centerId == null) {
            throw new BusinessException(ErrorCodeEnum.PROCESSING_CENTER_NOT_FOUND);
        }
        ProcessingCenterEntity center = processingCenterMapper.selectById(centerId);
        if (center == null || !Objects.equals(center.getStatus(), StatusConstants.NORMAL)
                || Objects.equals(center.getIsDeleted(), StatusConstants.YES)) {
            throw new BusinessException(ErrorCodeEnum.PROCESSING_CENTER_NOT_FOUND);
        }
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);
        DashboardVO vo = new DashboardVO();
        vo.setCards(buildCards(centerId, range));
        vo.setCharts(buildCharts(centerId, query));
        vo.setTodos(new ArrayList<>());
        return vo;
    }

    private List<CardVO> buildCards(Long centerId, ProductionDashboardQueryHelper.Range range) {
        List<CardVO> cards = new ArrayList<>();
        LambdaQueryWrapper<ProductionRecordEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .ge(ProductionRecordEntity::getCreateTime, range.startInclusive())
                .lt(ProductionRecordEntity::getCreateTime, range.endExclusive());
        Long total = productionRecordMapper.selectCount(totalWrapper);
        cards.add(CardVO.builder().key("totalProduction").title("生产单总数").value(total).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> inProductionWrapper = new LambdaQueryWrapper<>();
        inProductionWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .and(w -> w.between(ProductionRecordEntity::getStatus, 3010, 3090)
                        .or().between(ProductionRecordEntity::getStatus, 4010, 4090));
        Long inProduction = productionRecordMapper.selectCount(inProductionWrapper);
        cards.add(CardVO.builder().key("inProduction").title("生产中").value(inProduction).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> inQcWrapper = new LambdaQueryWrapper<>();
        inQcWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .between(ProductionRecordEntity::getStatus, 5010, 5090);
        Long inQc = productionRecordMapper.selectCount(inQcWrapper);
        cards.add(CardVO.builder().key("inQc").title("质检中").value(inQc).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> inWarehouseWrapper = new LambdaQueryWrapper<>();
        inWarehouseWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .in(ProductionRecordEntity::getStatus, 6010, 6020);
        Long inWarehouse = productionRecordMapper.selectCount(inWarehouseWrapper);
        cards.add(CardVO.builder().key("inWarehouse").title("仓储中").value(inWarehouse).unit("单").build());

        LambdaQueryWrapper<ProductionRecordEntity> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                .in(ProductionRecordEntity::getStatus, 6030, 8010)
                .isNotNull(ProductionRecordEntity::getPostProcessingEndTime)
                .ge(ProductionRecordEntity::getPostProcessingEndTime, range.startInclusive())
                .lt(ProductionRecordEntity::getPostProcessingEndTime, range.endExclusive());
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
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<Integer> inProductionData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> completedData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> otherStageData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        QueryWrapper<ProductionRecordEntity> inProductionWrapper = new QueryWrapper<>();
        inProductionWrapper.eq("processing_center_id", centerId)
                .and(w -> w.between("status", 3010, 3090).or().between("status", 4010, 4090))
                .ge("create_time", range.startInclusive()).lt("create_time", range.endExclusive());
        addGroupByClause(inProductionWrapper, query, range, "create_time");
        fillChartData(productionRecordMapper.selectMaps(inProductionWrapper), inProductionData, query, range);

        QueryWrapper<ProductionRecordEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.eq("processing_center_id", centerId).in("status", 6030, 8010)
                .isNotNull("post_processing_end_time")
                .ge("post_processing_end_time", range.startInclusive()).lt("post_processing_end_time", range.endExclusive());
        addGroupByClause(completedWrapper, query, range, "post_processing_end_time");
        fillChartData(productionRecordMapper.selectMaps(completedWrapper), completedData, query, range);

        QueryWrapper<ProductionRecordEntity> otherStageWrapper = new QueryWrapper<>();
        otherStageWrapper.eq("processing_center_id", centerId)
                .and(w -> w.between("status", 5010, 5090).or().between("status", 6010, 6020).or().eq("status", 9010))
                .ge("create_time", range.startInclusive()).lt("create_time", range.endExclusive());
        addGroupByClause(otherStageWrapper, query, range, "create_time");
        fillChartData(productionRecordMapper.selectMaps(otherStageWrapper), otherStageData, query, range);

        return ChartVO.builder().key("productionTrend").title("生产趋势").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        SeriesVO.builder().name("生产中").data(inProductionData).build(),
                        SeriesVO.builder().name("其他阶段").data(otherStageData).build(),
                        SeriesVO.builder().name("已完成").data(completedData).build())).build()).build();
    }

    private ChartVO buildWorkerWorkloadChart(Long centerId, DashboardQueryDTO query) {
        ProductionDashboardQueryHelper.Range range = ProductionDashboardQueryHelper.range(query);
        QueryWrapper<ProductionRecordEntity> backlogWrapper = new QueryWrapper<>();
        backlogWrapper.select("producer_id, MAX(producer_name) as producer_name, COUNT(*) as count")
                .eq("processing_center_id", centerId).isNotNull("producer_id")
                .and(w -> w.between("status", 3010, 3090).or().between("status", 4010, 4090))
                .groupBy("producer_id");
        QueryWrapper<ProductionRecordEntity> completedWrapper = new QueryWrapper<>();
        completedWrapper.select("producer_id, MAX(producer_name) as producer_name, COUNT(*) as count")
                .eq("processing_center_id", centerId).isNotNull("producer_id")
                .in("status", 6030, 8010).isNotNull("post_processing_end_time")
                .ge("post_processing_end_time", range.startInclusive())
                .lt("post_processing_end_time", range.endExclusive())
                .groupBy("producer_id");

        Map<Long, WorkerWorkload> workloads = new LinkedHashMap<>();
        mergeWorkload(productionRecordMapper.selectMaps(backlogWrapper), workloads, true);
        mergeWorkload(productionRecordMapper.selectMaps(completedWrapper), workloads, false);
        List<String> xAxis = new ArrayList<>();
        List<Integer> inProductionData = new ArrayList<>();
        List<Integer> completedData = new ArrayList<>();

        for (WorkerWorkload workload : workloads.values()) {
            xAxis.add(workload.name());
            inProductionData.add(workload.inProduction());
            completedData.add(workload.completed());
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

    private void mergeWorkload(List<Map<String, Object>> rows, Map<Long, WorkerWorkload> workloads, boolean backlog) {
        for (Map<String, Object> row : rows) {
            Long producerId = ((Number) row.get("producer_id")).longValue();
            String name = Objects.toString(row.get("producer_name"), "未知");
            int count = ((Number) row.get("count")).intValue();
            WorkerWorkload current = workloads.getOrDefault(producerId, new WorkerWorkload(name, 0, 0));
            workloads.put(producerId, backlog
                    ? new WorkerWorkload(name, current.inProduction() + count, current.completed())
                    : new WorkerWorkload(name, current.inProduction(), current.completed() + count));
        }
    }

    private record WorkerWorkload(String name, int inProduction, int completed) {
    }

}
