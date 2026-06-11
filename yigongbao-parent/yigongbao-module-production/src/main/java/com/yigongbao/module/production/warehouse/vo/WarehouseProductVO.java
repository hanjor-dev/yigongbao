package com.yigongbao.module.production.warehouse.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品明细VO
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Data
public class WarehouseProductVO {
    private Long productId;
    private String productNo;
    private String productName;
    private String specName;
    private String materialName;
    private String colorName;
    private String status;
    private String recordNo;
    private String orderNo;
    private String hospitalName;
    private String patientName;
    private LocalDateTime warehouseInTime;
    private String warehouseInRemark;
    private LocalDateTime warehouseOutTime;
    private String warehouseOutRemark;
}
