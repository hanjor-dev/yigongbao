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
     * 根据状态值获取显示名称
     *
     * @param status 状态值（0-禁用，1-正常）
     * @return 状态显示名称
     */
    public static String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        return status == NORMAL ? NORMAL_NAME : DISABLED_NAME;
    }

    // ==================== 账户分类 ====================

    /**
     * 账户分类-企业账户
     */
    public static final String ACCOUNT_TYPE_ENTERPRISE = "6.1";

    /**
     * 账户分类-业务账户
     */
    public static final String ACCOUNT_TYPE_BUSINESS = "6.2";

    /**
     * 账户分类-企业账户名称
     */
    public static final String ACCOUNT_TYPE_ENTERPRISE_NAME = "企业账户";

    /**
     * 账户分类-业务账户名称
     */
    public static final String ACCOUNT_TYPE_BUSINESS_NAME = "业务账户";

    /**
     * 根据账户分类值获取名称
     */
    public static String getAccountTypeName(String accountType) {
        if (accountType == null) {
            return "";
        }
        return ACCOUNT_TYPE_ENTERPRISE.equals(accountType) ? ACCOUNT_TYPE_ENTERPRISE_NAME : ACCOUNT_TYPE_BUSINESS_NAME;
    }

    // ==================== 操作结果 ====================

    /**
     * 操作结果-成功
     */
    public static final String SUCCESS_NAME = "成功";

    /**
     * 操作结果-失败
     */
    public static final String FAILURE_NAME = "失败";

    /**
     * 根据操作结果状态值获取显示名称
     *
     * @param status 状态值（0-失败，1-成功）
     * @return 操作结果显示名称
     */
    public static String getOperationResultName(Integer status) {
        if (status == null) {
            return "";
        }
        return status == NORMAL ? SUCCESS_NAME : FAILURE_NAME;
    }

    // ==================== 性别 ====================

    /**
     * 性别-男
     */
    public static final int SEX_MALE = 1;

    /**
     * 性别-女
     */
    public static final int SEX_FEMALE = 0;

    /**
     * 性别-男名称
     */
    public static final String SEX_MALE_NAME = "男";

    /**
     * 性别-女名称
     */
    public static final String SEX_FEMALE_NAME = "女";

    /**
     * 根据性别值获取名称
     */
    public static String getSexName(Integer sex) {
        if (sex == null) {
            return "";
        }
        return sex == SEX_MALE ? SEX_MALE_NAME : SEX_FEMALE_NAME;
    }

    // ==================== 设计确认状态 ====================

    /**
     * 设计文档确认状态-未确认
     */
    public static final int NOT_CONFIRMED = 0;

    /**
     * 设计文档确认状态-已确认
     */
    public static final int CONFIRMED = 1;

    // ==================== 部门类型 ====================

    /**
     * 部门类型-企业部门
     */
    public static final String DEPT_TYPE_ENTERPRISE = "6.1";

    /**
     * 部门类型-业务部门
     */
    public static final String DEPT_TYPE_BUSINESS = "6.2";

    /**
     * 部门类型-企业部门名称
     */
    public static final String DEPT_TYPE_ENTERPRISE_NAME = "企业部门";

    /**
     * 部门类型-业务部门名称
     */
    public static final String DEPT_TYPE_BUSINESS_NAME = "业务部门";

    /**
     * 根据部门类型值获取名称
     */
    public static String getDeptTypeName(String deptType) {
        if (deptType == null) {
            return "";
        }
        return DEPT_TYPE_ENTERPRISE.equals(deptType) ? DEPT_TYPE_ENTERPRISE_NAME : DEPT_TYPE_BUSINESS_NAME;
    }
}
