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

    /** 机构类型父级 dict_code = "1" */
    public static final String ORG_TYPE = "1";

    /** 生产企业 dict_code = "1.1" */
    public static final String ORG_TYPE_PRODUCER = "1.1";

    /** 经销商 dict_code = "1.2" */
    public static final String ORG_TYPE_DEALER = "1.2";

    /** 医疗机构 dict_code = "1.3" */
    public static final String ORG_TYPE_HOSPITAL = "1.3";

    /** 服务商 dict_code = "1.4" */
    public static final String ORG_TYPE_SERVICE_PROVIDER = "1.4";

    /** 代理产品线 dict_code = "5" */
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

    // ==================== 结算类型 ====================
    /** 结算类型父级 dict_code = "8" */
    public static final String SETTLEMENT_TYPE = "8";

    // ==================== 用户专业方向 ====================
    /**
     * 专业方向（父节点 dict_code）
     * 设计师/设计师管理员角色的 specialty 字段必须是此节点的子节点
     */
    public static final String USER_SPECIALTY = "7";

    /**
     * 通用专业方向（兜底编码）
     * 当订单专业方向无匹配设计师时，使用此编码进行二次查询
     */
    public static final String USER_SPECIALTY_GENERAL = "7.99";

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

    /**
     * 免费业务审批文件（业务类型为测试/试用时必须上传）
     */
    public static final String ORDER_FILE_CATEGORY_APPROVAL = "10.20";

    // ==================== 设计模块字典编码 ====================

    /**
     * 材质类型（父节点 dict_code）
     * 用于打印信息-材质选择
     */
    public static final String MATERIAL_TYPE = "15";

    /**
     * 材质类型-树脂（默认材质）
     * dict_code = "15.1"
     */
    public static final String MATERIAL_TYPE_RESIN = "15.1";

    /**
     * 颜色类型（父节点 dict_code）
     * 用于打印信息-颜色选择
     */
    public static final String COLOR_TYPE = "16";
}
