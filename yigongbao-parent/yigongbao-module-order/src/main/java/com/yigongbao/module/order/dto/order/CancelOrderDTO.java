package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消订单DTO
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Data
public class CancelOrderDTO {

    /**
     * 订单版本号（乐观锁）
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
