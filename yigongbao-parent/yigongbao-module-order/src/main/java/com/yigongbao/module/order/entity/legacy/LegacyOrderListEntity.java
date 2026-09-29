package com.yigongbao.module.order.entity.legacy;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("legacy_order_list")
public class LegacyOrderListEntity extends BaseEntity {
    private String sourceSystem;
    private Long sourceOrderId;
    private String sourceOrderCode;
    private String statusRaw;
    private String businessTypeRaw;
    private String printRaw;
    private String legacyDeptName;
    private String operatorName;
    private String operatorPhone;
    private String hospitalName;
    private String areaName;
    private String hospitalDeptName;
    private String doctorName;
    private String doctorPhone;
    private String patientName;
    private String patientAgeRaw;
    private String patientGenderRaw;
    private Integer postalFlag;
    private String postalAddressDisplay;
    private String designerName;
    private String expectedDeliveryRaw;
    private LocalDateTime expectedDeliveryTime;
    private String deliveryRaw;
    private LocalDateTime deliveryTime;
    private String estimatedCostRaw;
    private BigDecimal estimatedCostNumber;
    private String dataEvaluationOpinion;
    private String rebuildProjectSummary;
    private String rebuildProjectJson;
    private String designStartRaw;
    private LocalDateTime designStartTime;
    private String designSubmitRaw;
    private LocalDateTime designSubmitTime;
    private String productionStartRaw;
    private LocalDateTime productionStartTime;
    private String productionEndRaw;
    private LocalDateTime productionEndTime;
    private String createTimeRaw;
    private LocalDateTime sourceCreateTime;
    private String sourceRowHash;
    private LocalDateTime sourceSnapshotAt;
    private String mappingVersion;
}
