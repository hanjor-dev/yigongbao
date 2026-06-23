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
public class ProductionWorkerDashboardStrategy implements DashboardStrategy {
    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        DashboardVO vo = new DashboardVO();
        vo.setCards(List.of(
            CardVO.builder().key("myTasks").title("我的任务").value(0).unit("单").build(),
            CardVO.builder().key("completedTasks").title("已完成").value(0).unit("单").build(),
            CardVO.builder().key("qualityRate").title("质检通过率").value("0.0").unit("%").build(),
            CardVO.builder().key("avgProductionTime").title("平均生产时长").value("0").unit("小时").build()
        ));
        vo.setCharts(List.of(
            ChartVO.builder().key("yearProduction").title("同比生产对比").type("line")
                .data(LineChartDataVO.builder().xAxis(List.of("1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"))
                    .series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("今年").data(List.of(0,0,0,0,0,0,0,0,0,0,0,0)).build(),
                        LineChartDataVO.SeriesVO.builder().name("去年").data(List.of(0,0,0,0,0,0,0,0,0,0,0,0)).build()
                    )).build()).build(),
            ChartVO.builder().key("monthProduction").title("环比生产对比").type("line")
                .data(LineChartDataVO.builder().xAxis(List.of("第1周","第2周","第3周","第4周"))
                    .series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("本月").data(List.of(0,0,0,0)).build(),
                        LineChartDataVO.SeriesVO.builder().name("上月").data(List.of(0,0,0,0)).build()
                    )).build()).build(),
            ChartVO.builder().key("productionTrend").title("生产趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(List.of("暂无数据"))
                    .series(List.of(LineChartDataVO.SeriesVO.builder().name("产量").data(List.of(0)).build())).build()).build(),
            ChartVO.builder().key("reworkTrend").title("返工趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(List.of("暂无数据"))
                    .series(List.of(LineChartDataVO.SeriesVO.builder().name("返工数").data(List.of(0)).build())).build()).build()
        ));
        vo.setTodos(new ArrayList<>());
        return vo;
    }
}
