package com.yigongbao.module.order.vo.legacy;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LegacyOrderListVO {
    private Long id;
    private Long sourceOrderId;
    private String orderCode;
    private String status;
    private String businessTypeName;
    private String needsPhysicalDeliveryName;
    private String orgName;
    private String operatorName;
    private String operatorPhone;
    private String hospitalName;
    private String areaName;
    private String hospitalDeptName;
    private String doctorName;
    private String doctorPhone;
    private String patientName;
    private String patientAge;
    private String patientGenderName;
    private Integer isPostal;
    private String postalAddress;
    private String designerName;
    private String expectedDeliveryDate;
    private String deliveryDate;
    private String estimatedCost;
    private BigDecimal estimatedCostNumber;
    private String dataEvaluationOpinion;
    private String rebuildProjectSummary;
    private String designStartTime;
    private String designSubmitTime;
    private String productionStartTime;
    private String productionEndTime;
    private String createTime;
}
