package com.yigongbao.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记订单为经典案例请求DTO
 */
@Data
public class MarkClassicCaseDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "备注不能为空")
    private String remark;
}
