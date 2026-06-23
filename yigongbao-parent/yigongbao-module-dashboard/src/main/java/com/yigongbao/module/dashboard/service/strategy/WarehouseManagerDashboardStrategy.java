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
public class WarehouseManagerDashboardStrategy implements DashboardStrategy {
    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.warn("仓管数据概览暂不支持真实数据查询，需要 production_product 表支持");
        DashboardVO vo = new DashboardVO();
        vo.setCards(List.of(
            CardVO.builder().key("totalStock").title("库存总数").value(0).unit("件").build(),
            CardVO.builder().key("inboundToday").title("今日入库").value(0).unit("件").build(),
            CardVO.builder().key("outboundToday").title("今日出库").value(0).unit("件").build(),
            CardVO.builder().key("lowStockCount").title("低库存预警").value(0).unit("项").build()
        ));
        vo.setCharts(List.of(
            ChartVO.builder().key("stockTrend").title("出入库趋势").type("line")
                .data(LineChartDataVO.builder().xAxis(List.of("暂无数据"))
                    .series(List.of(
                        LineChartDataVO.SeriesVO.builder().name("入库").data(List.of(0)).build(),
                        LineChartDataVO.SeriesVO.builder().name("出库").data(List.of(0)).build()
                    )).build()).build(),
            ChartVO.builder().key("stockDistribution").title("库存分布").type("ring")
                .data(PieChartDataVO.builder().items(List.of(
                    PieChartDataVO.ItemVO.builder().name("暂无数据").value(0).build()
                )).build()).build()
        ));
        vo.setTodos(List.of(
            TodoVO.builder().id(1).title("低库存预警").count(0).link("/inventory?alert=low").urgent(true).build(),
            TodoVO.builder().id(2).title("待入库单据").count(0).link("/inbound?status=pending").build()
        ));
        return vo;
    }
}
