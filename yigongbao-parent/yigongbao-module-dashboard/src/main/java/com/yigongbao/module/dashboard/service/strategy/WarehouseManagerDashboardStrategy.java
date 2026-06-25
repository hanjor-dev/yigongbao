package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseManagerDashboardStrategy implements DashboardStrategy {
    private final ProductionProductMapper productMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建库管数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = new DashboardVO();
        try {
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
            vo.setCards(buildCards(range));
            vo.setCharts(buildCharts(query));
            vo.setTodos(new ArrayList<>());
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<CardVO> buildCards(LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        Long totalStock = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSED.getCode()));
        cards.add(CardVO.builder().key("totalStock").title("库存产品总数").value(totalStock).unit("件").build());

        Long inbound = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .isNotNull(ProductionProductEntity::getWarehouseInTime)
                .between(ProductionProductEntity::getWarehouseInTime, range[0], range[1]));
        cards.add(CardVO.builder().key("inbound").title("入库").value(inbound).unit("件").build());

        Long outbound = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .isNotNull(ProductionProductEntity::getWarehouseOutTime)
                .between(ProductionProductEntity::getWarehouseOutTime, range[0], range[1]));
        cards.add(CardVO.builder().key("outbound").title("出库").value(outbound).unit("件").build());

        return cards;
    }

    private List<ChartVO> buildCharts(DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildStockTrendChart(query));
        return charts;
    }

    private ChartVO buildStockTrendChart(DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<Integer> inboundData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));
        List<Integer> outboundData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        QueryWrapper<ProductionProductEntity> inWrapper = new QueryWrapper<>();
        inWrapper.between("warehouse_in_time", range[0], range[1]);
        addGroupByClause(inWrapper, query, range, "warehouse_in_time");
        fillChartData(productMapper.selectMaps(inWrapper), inboundData, query, range);

        QueryWrapper<ProductionProductEntity> outWrapper = new QueryWrapper<>();
        outWrapper.between("warehouse_out_time", range[0], range[1]);
        addGroupByClause(outWrapper, query, range, "warehouse_out_time");
        fillChartData(productMapper.selectMaps(outWrapper), outboundData, query, range);

        return ChartVO.builder().key("stockTrend").title("出入库趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(xAxis).series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("入库").data(inboundData).build(),
                        LineChartDataVO.SeriesVO.builder().name("出库").data(outboundData).build()
                )).build()).build();
    }

    private void addGroupByClause(QueryWrapper<ProductionProductEntity> wrapper, DashboardQueryDTO query, LocalDateTime[] range, String timeField) {
        switch (query.getTimeRangeEnum()) {
            case TODAY: wrapper.select("HOUR(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case WEEK: wrapper.select("DAYOFWEEK(" + timeField + ") as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
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
