package com.yigongbao.module.notification.dto;

import lombok.Data;

/**
 * 取消申请通知所需的最小业务数据，避免通知模块依赖订单模块。
 */
@Data
public class CancelApplyNotificationData {

    private String orderCode;
    private String applyReason;
    private String auditReason;
}
