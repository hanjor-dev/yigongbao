package com.yigongbao.module.order.vo.legacy;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 历史订单详情。保留归档时的原始文本，同时提供可解析的标准值，便于核对旧数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LegacyOrderDetailVO extends LegacyOrderListVO {
    private String sourceSystem;
    private String statusRaw;
    private String businessTypeRaw;
    private String printRaw;
    private String expectedDeliveryRaw;
    private String deliveryRaw;
    private String estimatedCostRaw;
    private BigDecimal estimatedCostNumber;
    private String patientAgeRaw;
    private String patientGenderRaw;
    private String designStartRaw;
    private String designSubmitRaw;
    private String productionStartRaw;
    private String productionEndRaw;
    private String createTimeRaw;
    private String postalSource;
    private String rebuildProjectJson;
    private String sourceRowHash;
    private String sourceSnapshotAt;
    private String mappingVersion;
    private String archiveCreatedAt;
    private String archiveUpdatedAt;
}
