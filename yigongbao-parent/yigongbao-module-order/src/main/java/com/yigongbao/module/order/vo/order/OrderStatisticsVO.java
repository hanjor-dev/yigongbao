package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单统计数据。
 */
@Data
public class OrderStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总订单数 */
    private long total;

    /** 待审核订单数 */
    private long pendingAudit;

    /** 设计中订单数 */
    private long designing;
}
