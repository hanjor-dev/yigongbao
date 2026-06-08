package com.yigongbao.module.order.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 时间窗口超限 VO
 * 用于返回订单修改时间窗口超限的详细信息
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class TimeWindowExceededVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间窗口（分钟）
     */
    private Integer timeWindow;

    /**
     * 已过时间（分钟）
     */
    private Long elapsedMinutes;

    /**
     * 是否需要申请（固定true）
     */
    private Boolean needApply;
}
