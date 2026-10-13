package com.yigongbao.module.dashboard.service.strategy;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.vo.DashboardVO;

/**
 * 数据概览策略接口
 */
public interface DashboardStrategy {
    /**
     * 构建数据概览
     * @param userId 用户ID
     * @param query 查询参数
     * @return 数据概览VO
     */
    DashboardVO buildDashboard(Long userId, DashboardQueryDTO query);
}
