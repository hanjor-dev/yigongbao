package com.yigongbao.module.production.record.vo;

import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import lombok.Data;
import java.math.BigDecimal;
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
    // 流转卡自身字段
    private Long id;
    private String recordNo;
    private Long orderId;
    private String orderCode;
    private Integer orderType;
    private Long designPackageId;
    private String designPackageCode;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    private String productionBatchNo;
    private String material;
    private String materialBatchNo;
    private Long processingCenterId;
    private String processingCenterName;
    private Long printDeviceId;
    private String printDeviceCode;
    private String printDeviceName;
    private Integer totalProductCount;
    private Integer qualifiedCount;
    private Integer unqualifiedCount;
    private Integer status;
    private String currentProcess;
    private String currentProcessName;
    private String qrCodeUrl;
    private LocalDateTime printStartTime;
    private LocalDateTime printFinishTime;
    private LocalDateTime postProcessingEndTime;
    private LocalDateTime createTime;
    // 订单冗余字段（来自 production_record 表）
    private String hospitalName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    private Integer isUrgent;
    private Integer isPostal;
    private LocalDateTime expectedDeliveryDate;
    // 订单关联字段（来自 order_main 关联查询）
    private Integer orderStatus;
    private Integer orderPhase;
    private String orgName;
    private String operatorName;
    private String operatorPhone;
    private String areaName;
    private String fullAreaName;
    private String operatorDeptName;
    private Integer patientAge;
    private String patientGender;
    private String postalAddress;
    private String designerName;
    private Long producerId;
    private String producerName;
    private BigDecimal estimatedCost;
    private LocalDateTime actualCompleteTime;
    private List<ProductionProductVO> products;
    // 设计文件（来自 design 模块关联查询）
    private FileVO instructionFile;
    private FileVO drawingFile;
    private FileVO dataPackageFile;
    private FileVO flowCardFile;
}
