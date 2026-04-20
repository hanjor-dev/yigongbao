package com.yigongbao.common.constant;

/**
 * 字典编码常量
 * 定义业务中使用的字典编码值
 *
 * @author hanjor
 * @date 2026-03-17
 */
public final class DictCodeConstants {

    private DictCodeConstants() {
    }

    // ==================== 机构相关字典编码 ====================

    /**
     * 机构类型父级字典code
     * dict_code = "1"
     */
    public static final String ORG_TYPE = "1";

    /**
     * 代理产品线
     * dict_code = "5"
     * 字典值：逗号分隔的多选值
     */
    public static final String AGENT_PRODUCT_LINE = "5";

    // ==================== 订单业务类型 ====================
    /**
     * 订单业务类型（父节点 dict_code）
     */
    public static final String ORDER_BUSINESS_TYPE = "11";

    /**
     * 业务
     */
    public static final String ORDER_BUSINESS_TYPE_BUSINESS = "11.1";

    /**
     * 测试
     */
    public static final String ORDER_BUSINESS_TYPE_TEST = "11.2";

    /**
     * 试用
     */
    public static final String ORDER_BUSINESS_TYPE_TRIAL = "11.3";

    /**
     * 代理
     */
    public static final String ORDER_BUSINESS_TYPE_AGENT = "11.4";

    // ==================== 患者性别 ====================
    /**
     * 患者性别（父节点 dict_code）
     */
    public static final String PATIENT_GENDER = "12";

    /**
     * 男
     */
    public static final String PATIENT_GENDER_MALE = "12.1";

    /**
     * 女
     */
    public static final String PATIENT_GENDER_FEMALE = "12.2";

    // ==================== 用户专业方向 ====================
    /**
     * 专业方向（父节点 dict_code）
     * 设计师/设计师管理员角色的 specialty 字段必须是此节点的子节点
     */
    public static final String USER_SPECIALTY = "7";

    // ==================== 重建项目分类 ====================
    /**
     * 重建项目分类（父节点 dict_code）
     */
    public static final String PROJECT_CATEGORY = "13";

    /**
     * 模型
     */
    public static final String PROJECT_CATEGORY_MODEL = "13.1";

    /**
     * 导板
     */
    public static final String PROJECT_CATEGORY_GUIDE = "13.2";

    /**
     * 假体
     */
    public static final String PROJECT_CATEGORY_IMPLANT = "13.3";

    /**
     * 其他
     */
    public static final String PROJECT_CATEGORY_OTHER = "13.4";

    // ==================== 订单文件分类 ====================

    /**
     * 订单文件分类（父节点 dict_code）
     * 对应 order_file.file_category 字段
     */
    public static final String ORDER_FILE_CATEGORY = "10";

    /**
     * 影像数据（DCM 压缩包）
     */
    public static final String ORDER_FILE_CATEGORY_DCM = "10.1";

    /**
     * 影像报告
     */
    public static final String ORDER_FILE_CATEGORY_REPORT = "10.2";
}
