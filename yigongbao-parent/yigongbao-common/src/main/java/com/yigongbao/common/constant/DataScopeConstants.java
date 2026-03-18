package com.yigongbao.common.constant;

/**
 * 数据范围常量
 * 定义角色数据范围的枚举值
 *
 * @author hanjor
 * @date 2026-03-17
 */
public final class DataScopeConstants {

    private DataScopeConstants() {
    }

    // ==================== 数据范围 ====================

    /**
     * 全部数据
     */
    public static final int ALL = 1;

    /**
     * 本机构
     */
    public static final int ORG = 2;

    /**
     * 仅自己
     */
    public static final int SELF = 3;

    /**
     * 医院范围
     */
    public static final int HOSPITALS = 4;

    /**
     * 部门范围
     */
    public static final int DEPT = 5;

    // ==================== 名称 ====================

    /**
     * 全部数据名称
     */
    public static final String ALL_NAME = "全部数据";

    /**
     * 本机构名称
     */
    public static final String ORG_NAME = "本机构";

    /**
     * 仅自己名称
     */
    public static final String SELF_NAME = "仅自己";

    /**
     * 医院范围名称
     */
    public static final String HOSPITALS_NAME = "医院范围";

    /**
     * 部门范围名称
     */
    public static final String DEPT_NAME = "部门范围";

    /**
     * 根据数据范围值获取名称
     *
     * @param dataScope 数据范围值
     * @return 数据范围名称
     */
    public static String getDataScopeName(Integer dataScope) {
        if (dataScope == null) {
            return "";
        }
        switch (dataScope) {
            case ALL:
                return ALL_NAME;
            case ORG:
                return ORG_NAME;
            case SELF:
                return SELF_NAME;
            case HOSPITALS:
                return HOSPITALS_NAME;
            case DEPT:
                return DEPT_NAME;
            default:
                return "";
        }
    }
}
