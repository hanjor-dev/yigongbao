package com.yigongbao.module.order.dto.modify;

import com.yigongbao.module.order.dto.draft.OrderItemDraftItemDTO;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 全量修改订单 DTO
 * 前端传入完整订单数据，后端自动判断变更内容
 *
 * @author hanjor
 * @date 2026-05-22
 */
@Data
public class OrderModifyFullDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 订单基本信息 ====================
    private Integer orderType;
    private String businessType;
    private Integer isPostal;
    private BigDecimal estimatedCost;
    private String dataEvaluationOpinion;

    // ==================== 患者信息 ====================
    private String patientName;
    private String patientGender;
    private Integer patientAge;

    // ==================== 医生信息 ====================
    private Long doctorId;
    private String doctorName;
    private String doctorPhone;

    // ==================== 医院科室 ====================
    private Long hospitalId;
    private Long hospitalDeptId;

    // ==================== 交付信息 ====================
    private Integer needsPhysicalDelivery;
    private String postalAddress;
    private LocalDateTime expectedDeliveryDate;
    private Integer isUrgent;

    // ==================== 重建项目（全量替换） ====================
    private List<OrderItemDraftItemDTO> items;

    // ==================== 影像文件（全量替换） ====================
    private List<String> imageDataFileIds;
    private List<String> imageReportFileIds;
}
