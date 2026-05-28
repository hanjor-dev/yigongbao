package com.yigongbao.module.production.record.vo;

import com.yigongbao.module.production.product.vo.ProductionProductVO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产流转卡 VO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionRecordVO {
    private Long id;
    private String recordNo;
    private Long orderId;
    private String orderCode;
    private Integer orderType;
    private String designPackageCode;
    private String productionBatchNo;
    private Integer totalProductCount;
    private Integer qualifiedCount;
    private Integer unqualifiedCount;
    private Integer hasRedoProduct;
    private Integer status;
    private String currentProcess;
    private String qrCodeUrl;
    private LocalDateTime createTime;
    // 订单基础信息
    private String hospitalName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    private Integer isUrgent;
    private Integer isPostal;
    private LocalDateTime expectedDeliveryDate;
    private List<ProductionProductVO> products;
}
