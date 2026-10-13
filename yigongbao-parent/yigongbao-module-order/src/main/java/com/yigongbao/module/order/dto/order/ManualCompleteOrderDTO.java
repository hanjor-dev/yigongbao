package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动完成订单DTO
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Data
public class ManualCompleteOrderDTO {

    /**
     * 订单版本号（乐观锁）
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}
