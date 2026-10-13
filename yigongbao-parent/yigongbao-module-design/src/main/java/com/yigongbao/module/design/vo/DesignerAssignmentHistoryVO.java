package com.yigongbao.module.design.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计师分配历史 VO
 *
 * @author hanjor
 * @date 2026-05-18
 */
@Data
@Schema(description = "设计师分配历史")
public class DesignerAssignmentHistoryVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderCode;

    @Schema(description = "虚拟单号")
    private String publicOrderCode;

    @Schema(description = "原设计师ID")
    private Long oldDesignerId;

    @Schema(description = "原设计师姓名")
    private String oldDesignerName;

    @Schema(description = "新设计师ID")
    private Long newDesignerId;

    @Schema(description = "新设计师姓名")
    private String newDesignerName;

    @Schema(description = "分配类型（AUTO=自动分配，MANUAL=手动分配）")
    private String assignType;

    @Schema(description = "分配时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注")
    private String remark;
}
