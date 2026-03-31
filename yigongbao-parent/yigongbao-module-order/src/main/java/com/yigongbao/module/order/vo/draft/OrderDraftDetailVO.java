package com.yigongbao.module.order.vo.draft;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 草稿详情 VO
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderDraftDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与操作人 ====================
    private Long id;
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
    private String operatorPhone;

    // ==================== 医院与科室 ====================
    private Long hospitalId;
    private String hospitalName;
    private Long deptId;
    private String deptName;

    // ==================== 医生信息 ====================
    private Long doctorId;
    private String doctorName;
    private String doctorPhone;

    // ==================== 患者信息 ====================
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientGenderName;

    // ==================== 业务信息 ====================
    private Integer isUrgent;
    private Integer isPostal;
    private String postalAddress;

    // ==================== 时效信息 ====================
    private LocalDateTime expectedDeliveryDate;

    // ==================== 有效期管理 ====================
    private LocalDateTime expiresAt;
    private Integer status;
    private String statusName;

    // ==================== 重建项目列表 ====================
    private List<OrderItemDraftVO> items;
    private Integer itemCount;

    // ==================== 时间信息 ====================
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ==================== 重建项目明细 VO ====================
    @Data
    public static class OrderItemDraftVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private Long bodyPartId;
        private String bodyPartName;
        private Long projectId;
        private String projectName;
        private BigDecimal projectEstimatedHours;
        private String projectDesc;
        private String formingRequirement;
        private String otherRequirement;
        private Integer sortOrder;
        private LocalDateTime createTime;
    }
}
