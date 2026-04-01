package com.yigongbao.module.order.dto.draft;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建/更新草稿 DTO
 * 包含草稿基础信息和重建项目列表
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class CreateOrderDraftDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 草稿ID（更新时传入） ====================
    /**
     * 草稿ID，更新时传入
     */
    private Long id;

    // ==================== 订单类型 ====================
    /**
     * 订单类型：1-医疗器械，2-非医疗器械
     */
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    /**
     * 是否需要实体交付：0-不需要，1-需要
     */
    @NotNull(message = "是否需要实体交付不能为空")
    @Min(value = 0, message = "是否需要实体交付值不合法")
    @Max(value = 1, message = "是否需要实体交付值不合法")
    private Integer needsPhysicalDelivery;

    /**
     * 业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    // ==================== 机构信息 ====================
    /**
     * 提单机构ID
     */
    @NotNull(message = "提单机构不能为空")
    private Long orgId;

    /**
     * 提单机构名称
     */
    private String orgName;

    /**
     * 操作员姓名
     */
    private String operatorName;

    /**
     * 操作员电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "操作员电话格式不正确")
    private String operatorPhone;

    // ==================== 医院与科室 ====================
    /**
     * 医院ID
     */
    @NotNull(message = "医院不能为空")
    private Long hospitalId;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 科室ID
     */
    private Long deptId;

    /**
     * 科室名称
     */
    private String deptName;

    // ==================== 医生/患者信息 ====================
    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 医生电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "医生电话格式不正确")
    private String doctorPhone;

    /**
     * 患者姓名
     */
    @NotBlank(message = "患者姓名不能为空")
    private String patientName;

    /**
     * 患者年龄
     */
    @Min(value = 0, message = "患者年龄不能为负数")
    @Max(value = 150, message = "患者年龄不能超过150")
    private Integer patientAge;

    /**
     * 患者性别（字典 dict_code：12.1-男，12.2-女）
     */
    private String patientGender;

    // ==================== 业务信息 ====================
    /**
     * 是否加急：0-否，1-是
     */
    @Min(value = 0, message = "是否加急值不合法")
    @Max(value = 1, message = "是否加急值不合法")
    private Integer isUrgent;

    /**
     * 是否邮寄：0-否，1-是
     */
    @Min(value = 0, message = "是否邮寄值不合法")
    @Max(value = 1, message = "是否邮寄值不合法")
    private Integer isPostal;

    /**
     * 邮寄地址
     */
    private String postalAddress;

    // ==================== 时效信息 ====================
    /**
     * 期望交付时间
     */
    private LocalDateTime expectedDeliveryDate;

    // ==================== 重建项目列表（嵌套） ====================
    /**
     * 重建项目列表
     */
    @Valid
    private List<OrderItemDraftItemDTO> items;
}
