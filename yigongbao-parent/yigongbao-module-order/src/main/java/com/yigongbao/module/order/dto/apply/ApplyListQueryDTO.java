package com.yigongbao.module.order.dto.apply;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 修改申请列表查询DTO
 *
 * @author hanjor
 * @since 2026-06-08
 */
@Data
public class ApplyListQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态：0=待审核，1=已通过，2=已驳回，3=已过期，null=全部
     */
    private Integer status;

    /**
     * 订单编号（可选）
     */
    private String orderCode;

    /**
     * 申请人姓名（可选）
     */
    private String applyUserName;

    /**
     * 申请开始时间（可选）
     */
    private LocalDateTime applyTimeStart;

    /**
     * 申请结束时间（可选）
     */
    private LocalDateTime applyTimeEnd;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}
