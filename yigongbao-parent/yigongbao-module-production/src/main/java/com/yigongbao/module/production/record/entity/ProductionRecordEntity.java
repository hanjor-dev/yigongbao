package com.yigongbao.module.production.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产流转卡实体
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_record")
public class ProductionRecordEntity extends BaseEntity {
    private String recordNo;
    private Long orderId;
    private String orderCode;
    private Integer orderType;
    private Long designPackageId;
    private String designPackageCode;
    private String productionBatchNo;
    private String versionNo;
    private String material;
    private Long processingCenterId;
    private String processingCenterName;
    private Long printDeviceId;
    private String printDeviceCode;
    private String printDeviceName;
    private Integer totalProductCount;
    private Integer qualifiedCount;
    private Integer unqualifiedCount;
    private Integer hasRedoProduct;
    private Integer status;
    private String currentProcess;
    private String qrCodeUrl;
    private Long packDeviceId;
    private String packDeviceNo;
    private BigDecimal packSealTemperature;
    private Integer packSealTime;
    private String packSterilizationMethod;
    private String packSterilizationBatchNo;
    private Long packOperatorId;
    private String packOperatorName;
    private LocalDateTime packTime;
    private String materialBatchNo;        // 原材料批号
    private LocalDateTime printStartTime;  // 打印开始时间
    private LocalDateTime printFinishTime; // 打印完成时间
}
