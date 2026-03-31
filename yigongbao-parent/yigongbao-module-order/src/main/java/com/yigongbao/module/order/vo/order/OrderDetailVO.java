package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderCode;
    private Integer orderType;
    private String businessType;
    private Long orgId;
    private String orgName;
    private Long operatorId;
    private String operatorName;
    private String operatorPhone;
    private Long hospitalId;
    private String hospitalName;
    private Long deptId;
    private String deptName;
    private Long doctorId;
    private String doctorName;
    private String doctorPhone;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String patientGenderName;
    private Integer isUrgent;
    private Integer isPostal;
    private String postalAddress;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime designStartTime;
    private LocalDateTime designSubmitTime;
    private LocalDateTime actualCompleteTime;
    private Integer phase;
    private Integer status;
    private Long currentHandlerId;
    private String currentHandlerName;
    private Long designerId;
    private Long producerId;
    private String auditRemark;
    private String designReviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrderItemVO> items;
    private Integer itemCount;
    private List<String> availableActions;

    /**
     * 订单明细 VO
     * 嵌套在 OrderDetailVO 中
     *
     * @author hanjor
     * @date 2026-03-31
     */
    @Data
    public static class OrderItemVO implements Serializable {
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
    }
}
