package com.yigongbao.module.dashboard.service;

import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.vo.DashboardVO;

/**
 * 数据概览服务接口
 */
public interface IDashboardService {
    /**
     * 获取数据概览
     * @param roleCode 角色代码
     * @param userId 用户ID
     * @param query 查询参数
     * @return 数据概览VO
     */
    DashboardVO getDashboard(String roleCode, Long userId, DashboardQueryDTO query);
}
