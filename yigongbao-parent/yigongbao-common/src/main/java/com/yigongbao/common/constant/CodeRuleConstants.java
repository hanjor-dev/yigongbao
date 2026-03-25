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

    // ==================== 基础模块编码 ====================

    /**
     * 医院编码
     */
    public static final String HOSPITAL_NO = "HOSPITAL_NO";

    /**
     * 机构部门编码
     */
    public static final String HDEPT_NO = "HDEPT_NO";

    /**
     * 重建项目编码
     */
    public static final String PROJECT_NO = "PROJECT_NO";

    /**
     * 产品型号编码
     */
    public static final String PRODUCT_CODE = "PRODUCT_CODE";

    /**
     * 部位编码
     */
    public static final String BODYPART_NO = "BODYPART_NO";

    /**
     * 模板编码
     */
    public static final String TEMPLATE_NO = "TEMPLATE_NO";

    /**
     * 医生编码
     */
    public static final String DOCTOR_NO = "DOCTOR_NO";

    // ==================== 订单相关编码 ====================

    /**
     * 订单编码
     */
    public static final String ORDER_NO = "ORDER_NO";

    /**
     * 订单明细编码
     */
    public static final String ORDER_ITEM_NO = "ORDER_ITEM_NO";

    /**
     * 数据包编码
     * 支持按业务前缀生成子序号，如 202603250001-1、202603250001-2
     */
    public static final String DATA_PACKAGE_NO = "DATA_PACKAGE_NO";

    /**
     * 指令单编码
     */
    public static final String INSTRUCTION_NO = "INSTRUCTION_NO";

    // ==================== 其他编码 ====================

    /**
     * 文件编码
     */
    public static final String FILE_NO = "FILE_NO";
}
