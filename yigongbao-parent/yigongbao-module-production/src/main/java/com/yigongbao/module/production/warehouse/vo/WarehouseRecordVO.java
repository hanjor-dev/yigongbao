package com.yigongbao.module.production.warehouse.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 仓储流转卡汇总信息
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Data
public class WarehouseRecordVO {
    private Long recordId;
    private String recordNo;
    private String orderNo;
    private String hospitalName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    private Integer isUrgent;
    private Integer isPostal;
    private LocalDateTime expectedDeliveryDate;
    private String processingCenterName;
    private String designPackageCode;
    private String productionBatchNo;
    private String materialBatchNo;
    private Integer totalCount;
    private Integer pendingWarehouseInCount;
    private Integer warehousedCount;
    private Integer warehouseOutCount;
    private Integer status;
    private LocalDateTime earliestInTime;
    private LocalDateTime latestOutTime;
}
