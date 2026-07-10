package com.yigongbao.module.order.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "取消申请详情VO")
public class CancelApplyVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderCode;

    @Schema(description = "申请人ID")
    private Long applyBy;

    @Schema(description = "申请人姓名")
    private String applyByName;

    @Schema(description = "取消原因")
    private String applyReason;

    @Schema(description = "审核状态：1=待审核，2=已通过，3=已驳回")
    private Integer auditStatus;

    @Schema(description = "审核人ID")
    private Long auditBy;

    @Schema(description = "审核人姓名")
    private String auditByName;

    @Schema(description = "审核原因")
    private String auditReason;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
