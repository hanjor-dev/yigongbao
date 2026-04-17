package com.yigongbao.module.design.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计审核历史记录 VO
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
public class DesignReviewHistoryVO {

    private Long id;

    /** 审核人姓名 */
    private String reviewerName;

    /** 审核结果：0=驳回，1=通过 */
    private Integer reviewResult;

    /** 审核结果名称 */
    private String reviewResultName;

    /** 审批意见（通过时） */
    private String comment;

    /** 驳回原因（驳回时） */
    private String rejectReason;

    /** 审核时间 */
    private LocalDateTime reviewTime;
}
