package com.yigongbao.module.order.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 提交取消申请DTO
 *
 * @author hanjor
 * @since 2026-07-10
 */
@Data
@Schema(description = "提交取消申请DTO")
public class CancelOrderApplyDTO {

    @Schema(description = "订单ID")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "取消原因（选填）")
    @Length(max = 500, message = "取消原因不能超过500字")
    private String reason;
}
