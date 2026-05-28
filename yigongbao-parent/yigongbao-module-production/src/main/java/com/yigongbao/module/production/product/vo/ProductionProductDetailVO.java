package com.yigongbao.module.production.product.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 生产产品明细 VO（分页列表）
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Data
public class ProductionProductDetailVO {

    // 产品自身字段
    private Long id;
    private String productNo;
    private String productName;
    private String fileName;
    private String status;
    private String qcResult;
    private String udiCode;
    private LocalDateTime createTime;

    // 流转卡信息
    private Long productionRecordId;
    private String recordNo;
    private String productionBatchNo;
    private Integer recordStatus;

    // 订单信息（来自 production_record 冗余字段）
    private Long orderId;
    private String orderCode;
    private Integer orderType;
    private String designPackageCode;
    private String hospitalName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    private Integer isUrgent;
    private Integer isPostal;
    private LocalDateTime expectedDeliveryDate;
}
