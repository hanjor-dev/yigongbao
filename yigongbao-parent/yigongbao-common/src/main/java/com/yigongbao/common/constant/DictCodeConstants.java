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
     * 机构类型
     * dict_code = "1"
     * 字典值：1-生产企业，2-经销商，3-医疗机构
     */
    public static final String ORG_TYPE = "1";

    /**
     * 医院等级
     * dict_code = "3"
     * 字典值：1-三级甲等，2-三级乙等，3-二级甲等等
     */
    public static final String HOSPITAL_LEVEL = "3";

    /**
     * 医院类型
     * dict_code = "4"
     * 字典值：1-综合医院，2-专科医院，3-社区医院等
     */
    public static final String HOSPITAL_TYPE = "4";

    /**
     * 代理产品线
     * dict_code = "5"
     * 字典值：逗号分隔的多选值
     */
    public static final String AGENT_PRODUCT_LINE = "5";
}
