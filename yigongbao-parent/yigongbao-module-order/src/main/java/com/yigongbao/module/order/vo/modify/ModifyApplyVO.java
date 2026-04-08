package com.yigongbao.module.order.vo.modify;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单修改申请 VO（创建后返回）
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ModifyApplyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderCode;
    /**
     * 申请类型字典编码，如 "14.1,14.3"
     */
    private String applyTypeCodes;
    /**
     * 申请类型中文名，如 "基础信息、重建项目"
     */
    private String applyTypeNames;
    private String applyReason;
    private String status;
    private String statusText;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime createTime;
}
