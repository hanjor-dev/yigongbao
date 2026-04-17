package com.yigongbao.common.constant;

/**
 * 编码规则编码常量
 * 统一管理所有业务模块的编码规则编码值
 *
 * @author hanjor
 * @date 2026-03-25
 */
public final class CodeRuleConstants {

    private CodeRuleConstants() {
    }

    // ==================== 系统模块编码 ====================

    /**
     * 部门编码（系统内部机构部门，区别于医院科室 HDEPT_NO）
     */
    public static final String DEPT_NO = "DEPT_NO";

    // ==================== 基础模块编码 ====================

    /**
     * 机构编码（generateWithCustomPrefix 调用，运行时拼接机构类型前缀）
     */
    public static final String ORG_NO = "ORG_NO";

    /**
     * 医院编码
     */
    public static final String HOSPITAL_NO = "HOSPITAL_NO";

    /**
     * 医院科室编码
     */
    public static final String HDEPT_NO = "HDEPT_NO";

    /**
     * 重建项目编码
     */
    public static final String PROJECT_NO = "PROJECT_NO";

    /**
     * 重建部位编码
     */
    public static final String BODYPART_NO = "BODYPART_NO";

    /**
     * 医院组合模板编码
     */
    public static final String TEMPLATE_NO = "TEMPLATE_NO";

    // ==================== 订单相关编码 ====================

    /**
     * 订单编码
     */
    public static final String ORDER_NO = "ORDER_NO";

    /**
     * 数据包编码（generateWithSeqSuffix 调用，按订单号隔离序号池）
     */
    public static final String DATA_PACKAGE_NO = "DATA_PACKAGE_NO";

    /**
     * 指令单编码
     */
    public static final String INSTRUCTION_NO = "INSTRUCTION_NO";
}
