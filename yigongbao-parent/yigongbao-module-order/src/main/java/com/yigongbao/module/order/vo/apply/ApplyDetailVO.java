package com.yigongbao.module.order.vo.apply;

import com.yigongbao.module.order.dto.diff.OrderModificationDiff;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 修改申请详情VO
 *
 * @author hanjor
 * @since 2026-06-08
 */
@Data
public class ApplyDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    private Long applyId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderCode;

    /**
     * 申请人姓名
     */
    private String applyUserName;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 变更差异
     */
    private OrderModificationDiff diff;
}
