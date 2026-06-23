package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.enums.TimeRangeEnum;
import com.yigongbao.module.dashboard.util.TimeRangeUtil;
import com.yigongbao.module.dashboard.vo.*;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductionManagerDashboardStrategy implements DashboardStrategy {
    private final OrderMainMapper orderMapper;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建生产管理员数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = new DashboardVO();
        vo.setCards(new ArrayList<>());
        vo.setCharts(new ArrayList<>());
        vo.setTodos(new ArrayList<>());
        return vo;
    }
}
