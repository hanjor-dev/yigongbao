package com.yigongbao.module.production.product.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产产品明细 VO（分页列表，含流转卡和订单冗余信息）
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Data
public class ProductionProductDetailVO {

    // ===== 产品自身字段 =====
    private Long id;
    /** 产品编号 */
    private String productNo;
    /** 产品名称 */
    private String productName;
    /** 打印文件名 */
    private String fileName;
    /** 产品重量，单位：克 */
    private BigDecimal weight;
    /** 产品状态代码 */
    private String status;
    /** 质检结果（pass/fail） */
    private String qcResult;
    /** UDI码（医疗器械） */
    private String udiCode;
    private LocalDateTime createTime;

    // ===== 流转卡信息 =====
    private Long productionRecordId;
    /** 流转卡编号 */
    private String recordNo;
    /** 生产批号 */
    private String productionBatchNo;
    /** 流转卡状态（对应 FlowStatusEnum 值） */
    private Integer recordStatus;

    // ===== 订单信息（来自 production_record 冗余字段）=====
    private Long orderId;
    private String orderCode;
    /** 订单类型（1=医疗器械，2=非医疗器械） */
    private Integer orderType;
    private String designPackageCode;
    private String hospitalName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    /** 是否加急（0=否，1=是） */
    private Integer isUrgent;
    /** 是否邮寄（0=否，1=是） */
    private Integer isPostal;
    private LocalDateTime expectedDeliveryDate;
}
