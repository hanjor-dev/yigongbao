package com.yigongbao.module.production.record.vo;

import lombok.Data;

/**
 * 流转卡取消预查询VO
 *
 * @author hanjor
 * @date 2026-05-29
 */
@Data
public class CancelPreviewVO {
    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderCode;
    /** 虚拟单号 */
    private String publicOrderCode;

    /** 订单下流转卡总数（未取消） */
    private Integer totalRecordCount;

    /** 提示信息 */
    private String message;
}
