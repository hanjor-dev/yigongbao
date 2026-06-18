package com.yigongbao.module.production.constants;

/**
 * 生产模块常量
 *
 * @author hanjor
 * @date 2026-05-27
 */
public class ProductionConstants {
    // 编码生成器规则常量（对应 sys_code_rule.rule_code 字段值，需在数据库中预置）
    public static final String PRODUCTION_RECORD_NO = "PRODUCTION_RECORD_NO";
    public static final String PRODUCTION_BATCH_NO = "PRODUCTION_BATCH_NO";
    public static final String PRODUCT_NO = "PRODUCT_NO";
    public static final String UDI_CODE = "UDI_CODE";

    // 订单类型常量
    public static final Integer ORDER_TYPE_MEDICAL = 1;
    public static final Integer ORDER_TYPE_NON_MEDICAL = 2;

    // 设备状态常量（0=空闲，非0=占用）
    public static final Integer DEVICE_STATE_IDLE = 0;
    public static final Integer DEVICE_STATE_BUSY = 1;  // 默认占用状态值

    private ProductionConstants() {}
}
