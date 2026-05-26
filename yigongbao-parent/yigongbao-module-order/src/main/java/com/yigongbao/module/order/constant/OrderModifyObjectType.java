package com.yigongbao.module.order.constant;

/**
 * 订单修改对象类型常量
 * 用于全量修改接口中的业务对象标识
 *
 * @author hanjor
 * @date 2026-05-22
 */
public final class OrderModifyObjectType {

    private OrderModifyObjectType() {
    }

    /** 患者信息 */
    public static final String PATIENT = "patient";

    /** 医生信息 */
    public static final String DOCTOR = "doctor";

    /** 医院科室 */
    public static final String HOSPITAL = "hospital";

    /** 交付信息 */
    public static final String DELIVERY = "delivery";

    /** 重建项目 */
    public static final String ITEMS = "items";

    /** 影像文件 */
    public static final String IMAGES = "images";

    /** 订单基本信息（类型/业务类型） */
    public static final String ORDER_INFO = "orderInfo";
}
