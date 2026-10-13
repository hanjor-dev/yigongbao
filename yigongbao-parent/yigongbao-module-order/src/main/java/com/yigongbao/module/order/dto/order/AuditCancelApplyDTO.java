package com.yigongbao.module.order.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 审核取消申请DTO
 *
 * @author hanjor
 * @since 2026-07-10
 */
@Data
@Schema(description = "审核取消申请DTO")
public class AuditCancelApplyDTO {

    @Schema(description = "审核结果：true=通过，false=驳回")
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核备注（驳回时选填）")
    @Length(max = 500, message = "审核备注不能超过500字")
    private String reason;
}
