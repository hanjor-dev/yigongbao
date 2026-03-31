package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单列表项 VO
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderCode;
    private Integer orderType;
    private String businessType;
    private Long hospitalId;
    private String hospitalName;
    private String patientName;
    private Integer phase;
    private Integer status;
    private LocalDateTime createTime;
}
