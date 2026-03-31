package com.yigongbao.module.order.dto.order;

import lombok.Data;

import java.io.Serializable;

/**
 * 审核订单 DTO
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class AuditOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String remark;
}
