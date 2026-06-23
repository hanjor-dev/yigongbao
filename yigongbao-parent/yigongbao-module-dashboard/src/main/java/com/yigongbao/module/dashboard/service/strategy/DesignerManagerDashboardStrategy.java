package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.order.mapper.OrderMainMapper;
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
public class DesignerManagerDashboardStrategy implements DashboardStrategy {
    private final OrderMainMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建设计管理员数据概览: userId={}, query={}", userId, query);
        try {
            TimeRangeEnum timeRange = query.getTimeRangeEnum();
            LocalDateTime[] range = com.yigongbao.module.dashboard.util.TimeRangeUtil.getStartAndEndTime(timeRange, query.getStartDate(), query.getEndDate());
            UserEntity user = userMapper.selectById(userId);

            List<Long> designerIds;
            if (user != null && user.getDeptId() != null) {
                designerIds = userMapper.selectList(
                    new QueryWrapper<UserEntity>().eq("dept_id", user.getDeptId())
                ).stream().map(UserEntity::getId).collect(Collectors.toList());
            } else {
                designerIds = null;
            }

            DashboardVO vo = new DashboardVO();
            vo.setCards(buildCards(designerIds, range));
            vo.setCharts(buildCharts(designerIds, range));
            vo.setTodos(new ArrayList<>());
            return vo;
        } catch (Exception e) {
            log.error("构建设计管理员数据概览失败: userId={}, query={}", userId, query, e);
            return buildEmptyDashboard();
        }
    }

    private List<CardVO> buildCards(List<Long> designerIds, LocalDateTime[] range) {
        QueryWrapper<OrderMainEntity> baseWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            baseWrapper.in("designer_id", designerIds);
        }
        long teamTotal = orderMapper.selectCount(baseWrapper
            .ge("phase", 20)
            .ne("status", 9010)
            .between("create_time", range[0], range[1]));

        long unassigned = orderMapper.selectCount(new QueryWrapper<OrderMainEntity>()
            .ge("phase", 20)
            .and(w -> w.isNull("designer_id").or().eq("designer_id", "")));

        QueryWrapper<OrderMainEntity> designingWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            designingWrapper.in("designer_id", designerIds);
        }
        long designing = orderMapper.selectCount(designingWrapper.eq("status", 2020));

        QueryWrapper<OrderMainEntity> completedWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            completedWrapper.in("designer_id", designerIds);
        }
        long designCompleted = orderMapper.selectCount(completedWrapper
            .ge("status", 2030)
            .ne("status", 9010)
            .between("create_time", range[0], range[1]));

        QueryWrapper<OrderMainEntity> pendingAuditWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            pendingAuditWrapper.in("designer_id", designerIds);
        }
        pendingAuditWrapper.and(w -> w.eq("status", 1020).or().eq("design_audit_status", 0));
        long pendingAudit = orderMapper.selectCount(pendingAuditWrapper);

        return List.of(
            CardVO.builder().key("teamTotal").title("团队工单总数").value(teamTotal).unit("单").build(),
            CardVO.builder().key("unassigned").title("待分配工单").value(unassigned).unit("单").build(),
            CardVO.builder().key("designing").title("设计中订单").value(designing).unit("单").build(),
            CardVO.builder().key("designCompleted").title("设计完成订单").value(designCompleted).unit("单").build(),
            CardVO.builder().key("pendingAudit").title("待审核订单").value(pendingAudit).unit("单").build()
        );
    }

    private List<ChartVO> buildCharts(List<Long> designerIds, LocalDateTime[] range) {
        return List.of(
            buildDesignerWorkloadChart(designerIds),
            buildWorkflowFunnelChart(designerIds)
        );
    }

    private ChartVO buildDesignerWorkloadChart(List<Long> designerIds) {
        QueryWrapper<OrderMainEntity> wrapper = new QueryWrapper<>();
        wrapper.select("designer_id, designer_name, " +
                "SUM(CASE WHEN status = 2010 THEN 1 ELSE 0 END) as pending, " +
                "SUM(CASE WHEN status = 2020 THEN 1 ELSE 0 END) as designing, " +
                "SUM(CASE WHEN status >= 2030 AND status != 9010 THEN 1 ELSE 0 END) as completed")
               .ge("phase", 20)
               .isNotNull("designer_id")
               .ne("designer_id", "");

        if (designerIds != null && !designerIds.isEmpty()) {
            wrapper.in("designer_id", designerIds);
        }

        wrapper.groupBy("designer_id")
               .orderByDesc("pending + designing + completed")
               .last("LIMIT 10");

        List<Map<String, Object>> results = orderMapper.selectMaps(wrapper);
        List<String> names = new ArrayList<>();
        List<Integer> pendingData = new ArrayList<>();
        List<Integer> designingData = new ArrayList<>();
        List<Integer> completedData = new ArrayList<>();

        for (Map<String, Object> row : results) {
            names.add((String) row.get("designer_name"));
            pendingData.add(((Number) row.get("pending")).intValue());
            designingData.add(((Number) row.get("designing")).intValue());
            completedData.add(((Number) row.get("completed")).intValue());
        }

        if (names.isEmpty()) {
            names.add("暂无数据");
            pendingData.add(0);
            designingData.add(0);
            completedData.add(0);
        }

        return ChartVO.builder()
            .key("designerWorkload").title("设计师工作负载").type("bar")
            .data(LineChartDataVO.builder().xAxis(names).series(List.of(
                LineChartDataVO.SeriesVO.builder().name("待设计").data(pendingData).build(),
                LineChartDataVO.SeriesVO.builder().name("设计中").data(designingData).build(),
                LineChartDataVO.SeriesVO.builder().name("设计完成").data(completedData).build()
            )).build())
            .build();
    }

    private ChartVO buildWorkflowFunnelChart(List<Long> designerIds) {
        QueryWrapper<OrderMainEntity> pendingWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            pendingWrapper.in("designer_id", designerIds);
        }
        long pending = orderMapper.selectCount(pendingWrapper.eq("status", 2010));

        QueryWrapper<OrderMainEntity> designingWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            designingWrapper.in("designer_id", designerIds);
        }
        long designing = orderMapper.selectCount(designingWrapper.eq("status", 2020));

        QueryWrapper<OrderMainEntity> completedWrapper = new QueryWrapper<>();
        if (designerIds != null && !designerIds.isEmpty()) {
            completedWrapper.in("designer_id", designerIds);
        }
        long completed = orderMapper.selectCount(completedWrapper.ge("status", 2030).ne("status", 9010));

        return ChartVO.builder()
            .key("workflowFunnel").title("工单流转漏斗图").type("funnel")
            .data(PieChartDataVO.builder().items(List.of(
                PieChartDataVO.ItemVO.builder().name("待设计").value((int) pending).build(),
                PieChartDataVO.ItemVO.builder().name("设计中").value((int) designing).build(),
                PieChartDataVO.ItemVO.builder().name("设计完成").value((int) completed).build()
            )).build())
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
