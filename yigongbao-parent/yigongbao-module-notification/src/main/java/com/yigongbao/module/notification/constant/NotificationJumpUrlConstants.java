package com.yigongbao.module.notification.constant;

/**
 * 消息通知跳转链接常量
 *
 * @author hanjor
 * @date 2026-06-18
 */
public class NotificationJumpUrlConstants {

    /**
     * 订单详情页（需拼接 orderId）
     * 示例：/order/detail/123
     */
    public static final String ORDER_DETAIL = "/order/detail/";

    /**
     * 设计工单列表页
     */
    public static final String DESIGN_LIST = "/design/list";

    /**
     * 生产流转卡详情页（需拼接 recordId）
     * 示例：/production/record/456
     */
    public static final String PRODUCTION_RECORD = "/production/record/";

    private NotificationJumpUrlConstants() {
    }
}
