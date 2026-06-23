package com.yigongbao.module.dashboard.service.strategy;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class QcDashboardStrategy implements DashboardStrategy {
    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.warn("质检员数据概览暂不支持真实数据查询，需要 production_product 表支持");
        DashboardVO vo = new DashboardVO();
        vo.setCards(List.of(
            CardVO.builder().key("totalQC").title("质检总数").value(0).unit("单").build(),
            CardVO.builder().key("passCount").title("通过数量").value(0).unit("单").build(),
            CardVO.builder().key("failCount").title("不合格数量").value(0).unit("单").build(),
            CardVO.builder().key("passRate").title("通过率").value("0.0").unit("%").build()
        ));
        vo.setCharts(List.of(
            ChartVO.builder().key("qcResultTrend").title("质检结果趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(List.of("暂无数据"))
                    .series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("通过").data(List.of(0)).build(),
                        LineChartDataVO.SeriesVO.builder().name("不合格").data(List.of(0)).build()
                    )).build()).build(),
            ChartVO.builder().key("productTypeDistribution").title("产品类型分布").type("pie")
                .data(PieChartDataVO.builder().items(List.of(
                    PieChartDataVO.ItemVO.builder().name("暂无数据").value(0).build()
                )).build()).build()
        ));
        vo.setTodos(new ArrayList<>());
        return vo;
    }
}
