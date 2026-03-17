package com.yigongbao.common.constant;

/**
 * 系统状态常量
 * 统一管理项目中使用的状态值（0和1）
 *
 * @author hanjor
 * @date 2026-03-17
 */
public final class StatusConstants {

    private StatusConstants() {
    }

    // ==================== 通用状态 ====================

    /**
     * 正常/启用状态
     */
    public static final int NORMAL = 1;

    /**
     * 禁用/停用状态
     */
    public static final int DISABLED = 0;

    /**
     * 状态-正常（整数值）
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 状态-禁用（整数值）
     */
    public static final int STATUS_DISABLED = 0;

    // ==================== 逻辑删除 ====================

    /**
     * 未删除
     */
    public static final int NOT_DELETED = 0;

    /**
     * 已删除
     */
    public static final int DELETED = 1;

    // ==================== 布尔判断 ====================

    /**
     * 是/True
     */
    public static final int YES = 1;

    /**
     * 否/False
     */
    public static final int NO = 0;

    // ==================== 状态名称 ====================

    /**
     * 正常状态对应的显示名称
     */
    public static final String NORMAL_NAME = "正常";

    /**
     * 禁用状态对应的显示名称
     */
    public static final String DISABLED_NAME = "禁用";

    /**
     * 根据状态值获取状态名称
     *
     * @param status 状态值
     * @return 状态名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        return status == NORMAL ? NORMAL_NAME : DISABLED_NAME;
    }
}
