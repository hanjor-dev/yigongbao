package com.yigongbao.module.order.vo.draft;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 草稿列表项 VO
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderDraftVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键 ====================
    private Long id;

    // ==================== 操作人信息 ====================
    private Long operatorId;
    private String operatorName;

    // ==================== 订单类型 ====================
    private Integer orderType;
    private String orderTypeName;
    private String businessType;
    private String businessTypeName;

    // ==================== 机构信息 ====================
    private Long orgId;
    private String orgName;

    // ==================== 医院信息 ====================
    private Long hospitalId;
    private String hospitalName;

    // ==================== 患者信息 ====================
    private String patientName;
    private String patientGender;
    private String patientGenderName;

    // ==================== 业务信息 ====================
    private Integer isUrgent;
    private Integer isPostal;

    // ==================== 有效期管理 ====================
    private LocalDateTime expiresAt;
    private Integer status;
    private String statusName;

    private Integer itemCount;

    // ==================== 时间信息 ====================
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
