package com.yigongbao.module.dashboard.service.strategy;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompanyAdminDashboardStrategy implements DashboardStrategy {
    private final SuperAdminDashboardStrategy superAdminStrategy;

    @Override
    public DashboardVO buildDashboard(Long userId, DashboardQueryDTO query) {
        log.info("构建公司管理员数据概览: userId={}, query={}", userId, query);
        DashboardVO vo = superAdminStrategy.buildDashboard(userId, query);
        vo.setSystem(null);
        return vo;
    }
}
