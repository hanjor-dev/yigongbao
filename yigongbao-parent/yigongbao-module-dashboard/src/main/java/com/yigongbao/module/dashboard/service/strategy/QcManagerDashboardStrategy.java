package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QcManagerDashboardStrategy implements DashboardStrategy {
    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final UserMapper userMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建质检管理员数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = new DashboardVO();
        try {
            LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
            List<Long> qcUserIds = getQcUserIds();
            vo.setCards(buildCards(qcUserIds, range));
            vo.setCharts(buildCharts(qcUserIds, query));
            vo.setTodos(new ArrayList<>());
        } catch (Exception e) {
            log.error("构建数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
        return vo;
    }

    private List<Long> getQcUserIds() {
        return userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .in(UserEntity::getRoleCode, RoleCodeEnum.QC_WORKER.getCode(), RoleCodeEnum.QC_MANAGER.getCode())
                .select(UserEntity::getId))
                .stream().map(UserEntity::getId).collect(Collectors.toList());
    }

    private List<CardVO> buildCards(List<Long> qcUserIds, LocalDateTime[] range) {
        List<CardVO> cards = new ArrayList<>();

        // 待质检生产单（状态为质检中，不限定质检员）
        LambdaQueryWrapper<ProductionRecordEntity> pendingQcWrapper = new LambdaQueryWrapper<>();
        pendingQcWrapper.eq(ProductionRecordEntity::getStatus, FlowStatusEnum.QC_IN_PROGRESS.getValue())
                .between(ProductionRecordEntity::getCreateTime, range[0], range[1]);
        Long pendingQc = recordMapper.selectCount(pendingQcWrapper);
        cards.add(CardVO.builder().key("pendingQc").title("待质检生产单").value(pendingQc).unit("单").build());

        if (qcUserIds.isEmpty()) {
            cards.add(CardVO.builder().key("totalQcProducts").title("质检产品总数").value(0L).unit("件").build());
            cards.add(CardVO.builder().key("passProducts").title("合格产品数").value(0L).unit("件").build());
            cards.add(CardVO.builder().key("failProducts").title("不合格产品数").value(0L).unit("件").build());
            return cards;
        }

        LambdaQueryWrapper<ProductionProductEntity> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.in(ProductionProductEntity::getQcUserId, qcUserIds)
                .between(ProductionProductEntity::getQcTime, range[0], range[1]);
        Long total = productMapper.selectCount(totalWrapper);
        cards.add(CardVO.builder().key("totalQcProducts").title("质检产品总数").value(total).unit("件").build());

        LambdaQueryWrapper<ProductionProductEntity> passWrapper = new LambdaQueryWrapper<>();
        passWrapper.in(ProductionProductEntity::getQcUserId, qcUserIds)
                .eq(ProductionProductEntity::getQcResult, "pass")
                .between(ProductionProductEntity::getQcTime, range[0], range[1]);
        Long pass = productMapper.selectCount(passWrapper);
        cards.add(CardVO.builder().key("passProducts").title("合格产品数").value(pass).unit("件").build());

        LambdaQueryWrapper<ProductionProductEntity> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.in(ProductionProductEntity::getQcUserId, qcUserIds)
                .ne(ProductionProductEntity::getQcResult, "pass")
                .isNotNull(ProductionProductEntity::getQcResult)
                .between(ProductionProductEntity::getQcTime, range[0], range[1]);
        Long fail = productMapper.selectCount(failWrapper);
        cards.add(CardVO.builder().key("failProducts").title("不合格产品数").value(fail).unit("件").build());

        return cards;
    }

    private List<ChartVO> buildCharts(List<Long> qcUserIds, DashboardQueryDTO query) {
        List<ChartVO> charts = new ArrayList<>();
        charts.add(buildQcTrendChart(qcUserIds, query));
        charts.add(buildWorkerWorkloadChart(qcUserIds, query));
        return charts;
    }

    private ChartVO buildQcTrendChart(List<Long> qcUserIds, DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<String> xAxis = TimeRangeUtil.getXAxisLabels(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        List<Integer> qcData = new ArrayList<>(Collections.nCopies(xAxis.size(), 0));

        if (!qcUserIds.isEmpty()) {
            QueryWrapper<ProductionProductEntity> wrapper = new QueryWrapper<>();
            wrapper.in("qc_user_id", qcUserIds).between("qc_time", range[0], range[1]);
            addGroupByClause(wrapper, query, range);
            fillChartData(productMapper.selectMaps(wrapper), qcData, query, range);
        }

        return ChartVO.builder().key("qcTrend").title("产品质检趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(xAxis).series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("质检产品数").data(qcData).build())).build()).build();
    }

    private ChartVO buildWorkerWorkloadChart(List<Long> qcUserIds, DashboardQueryDTO query) {
        LocalDateTime[] range = TimeRangeUtil.getStartAndEndTime(query.getTimeRangeEnum(), query.getStartDate(), query.getEndDate());
        QueryWrapper<ProductionProductEntity> wrapper = new QueryWrapper<>();
        wrapper.select("qc_user_id, COUNT(*) as count")
                .in("qc_user_id", qcUserIds.isEmpty() ? Collections.singletonList(-1L) : qcUserIds)
                .between("qc_time", range[0], range[1])
                .groupBy("qc_user_id");

        List<Map<String, Object>> results = productMapper.selectMaps(wrapper);
        Map<Long, String> userNameMap = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>().in(UserEntity::getId, qcUserIds.isEmpty() ? Collections.singletonList(-1L) : qcUserIds))
                .stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getRealName, (a, b) -> a));

        List<String> xAxis = new ArrayList<>();
        List<Integer> workloadData = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Long userId = ((Number) row.get("qc_user_id")).longValue();
            int count = ((Number) row.get("count")).intValue();
            xAxis.add(userNameMap.getOrDefault(userId, "未知"));
            workloadData.add(count);
        }
        if (xAxis.isEmpty()) {
            xAxis.add("暂无数据");
            workloadData.add(0);
        }

        return ChartVO.builder().key("workerWorkload").title("质检员工作量").type("bar")
                .data(BarChartDataVO.builder().xAxis(xAxis).series(List.of(
                        BarChartDataVO.SeriesVO.builder().name("质检产品数").data(workloadData).build())).build()).build();
    }

    private void addGroupByClause(QueryWrapper<ProductionProductEntity> wrapper, DashboardQueryDTO query, LocalDateTime[] range) {
        switch (query.getTimeRangeEnum()) {
            case TODAY: wrapper.select("HOUR(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
            case WEEK: wrapper.select("WEEKDAY(qc_time) as time_unit, COUNT(*) as count").groupBy("time_unit"); break;
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
