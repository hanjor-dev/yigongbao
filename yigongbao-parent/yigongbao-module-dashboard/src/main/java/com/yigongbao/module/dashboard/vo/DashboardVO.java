package com.yigongbao.module.dashboard.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 数据概览响应体
 */
@Data
public class DashboardVO {
    /**
     * KPI 卡片列表
     */
    private List<CardVO> cards;

    /**
     * 图表配置列表
     */
    private List<ChartVO> charts;

    /**
     * 待办事项列表
     */
    private List<TodoVO> todos;

    /**
     * 系统监控（仅 super_admin）
     */
    private SystemVO system;
}
