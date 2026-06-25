package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class QcWorkerDashboardStrategy implements DashboardStrategy {
    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建QC质检员数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = new DashboardVO();
        try {
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
            vo.setCards(buildCards(userId, range));
            vo.setCharts(buildCharts(userId, query));
            vo.setTodos(new ArrayList<>());
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<CardVO> buildCards(Long userId, LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        // 待质检生产单（状态为质检中，不限定质检员）
        LambdaQueryWrapper<ProductionRecordEntity> pendingQcWrapper = new LambdaQueryWrapper<>();
        pendingQcWrapper.eq(ProductionRecordEntity::getStatus, FlowStatusEnum.QC_IN_PROGRESS.getValue())
                .between(ProductionRecordEntity::getCreateTime, range[0], range[1]);
        Long pendingQc = recordMapper.selectCount(pendingQcWrapper);
        cards.add(CardVO.builder().key("pendingQc").title("待质检生产单").value(pendingQc).unit("单").build());

        // 质检产品总数
        LambdaQueryWrapper<ProductionProductEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(ProductionProductEntity::getQcUserId, userId)
                .between(ProductionProductEntity::getQcTime, range[0], range[1]);
        Long total = productMapper.selectCount(totalWrapper);
        cards.add(CardVO.builder().key("totalQcProducts").title("质检产品总数").value(total).unit("件").build());

        // 合格产品数
        LambdaQueryWrapper<ProductionProductEntity> passWrapper = new LambdaQueryWrapper<>();
        passWrapper.eq(ProductionProductEntity::getQcUserId, userId)
                .eq(ProductionProductEntity::getQcResult, "pass")
                .between(ProductionProductEntity::getQcTime, range[0], range[1]);
        Long pass = productMapper.selectCount(passWrapper);
        cards.add(CardVO.builder().key("passProducts").title("合格产品数").value(pass).unit("件").build());

        // 不合格产品数
        LambdaQueryWrapper<ProductionProductEntity> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.eq(ProductionProductEntity::getQcUserId, userId)
                .ne(ProductionProductEntity::getQcResult, "pass")
                .isNotNull(ProductionProductEntity::getQcResult)
                .between(ProductionProductEntity::getQcTime, range[0], range[1]);
        Long fail = productMapper.selectCount(failWrapper);
        cards.add(CardVO.builder().key("failProducts").title("不合格产品数").value(fail).unit("件").build());

        return cards;
    }

    private List<ChartVO> buildCharts(Long userId, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildQcTrendChart(userId, query));
        return charts;
    }

    private ChartVO buildQcTrendChart(Long userId, DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<Integer> qcData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        QueryWrapper<ProductionProductEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("qc_user_id", userId).between("qc_time", range[0], range[1]);
        addGroupByClause(wrapper, query, range);
        fillChartData(productMapper.selectMaps(wrapper), qcData, query, range);

        return ChartVO.builder().key("qcTrend").title("产品质检趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(xAxis).series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("质检产品数").data(qcData).build())).build()).build();
    }

    private void addGroupByClause(QueryWrapper<ProductionProductEntity> wrapper, DashboardQueryDTO query, LocalDateTime[] range) {
        switch (query.getTimeRangeEnum()) {
            case TODAY: wrapper.select("HOUR(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case WEEK: wrapper.select("DAYOFWEEK(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case MONTH: wrapper.select("DAY(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case QUARTER:
            case YEAR: wrapper.select("MONTH(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case CUSTOM:
                long days = java.time.temporal.ChronoUnit.DAYS.between(range[0].toLocalDate(), range[1].toLocalDate()) + 1;
                if (days <= 1) {
                    wrapper.select("HOUR(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 7) {
                    wrapper.select("DATEDIFF(qc_time, '" + range[0].toLocalDate() + "') as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else if (days <= 31) {
                    wrapper.select("DAY(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
                } else {
                    wrapper.select("MONTH(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit");
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
