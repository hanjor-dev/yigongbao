package com.yigongbao.module.production.warehouse.vo;

import com.yigongbao.common.vo.StatusColorVO;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 仓储详情VO
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Data
public class WarehouseDetailVO {
    private Long recordId;
    private String recordNo;
    private String orderCode;
    private String publicOrderCode;
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
    private Integer status;
    /** 流转卡状态标签颜色 */
    private StatusColorVO statusColor;
    private Integer totalCount;
    private Integer pendingWarehouseInCount;
    private Integer warehousedCount;
    private Integer warehouseOutCount;
    private Integer cancelledCount;
    private List<ProductionProductVO> products;
}
