package com.yigongbao.module.notification.constant;

/**
 * 消息通知跳转链接常量
 *
 * @author hanjor
 * @date 2026-06-18
 */
public class NotificationJumpUrlConstants {

    /** 订单列表页，前端自动拼 ?detailId=orderId */
    public static final String ORDER_DETAIL = "/order";

    /** 设计工单页，bizId = orderId */
    public static final String DESIGN_LIST = "/design";

    /** 订单修改申请页，bizId = applyId */
    public static final String MODIFY_APPLY = "/modifyApply";

    /** 生产流转卡页，bizId = recordId */
    public static final String PRODUCTION_RECORD = "/manufacture";

    private NotificationJumpUrlConstants() {
    }
}
