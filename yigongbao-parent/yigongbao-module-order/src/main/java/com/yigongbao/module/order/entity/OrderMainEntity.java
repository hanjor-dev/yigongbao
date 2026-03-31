package com.yigongbao.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单主表 Entity
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
@TableName("order_main")
@EqualsAndHashCode(callSuper = false)
public class OrderMainEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 主键与编码 ====================
    /**
     * 订单编号
     */
    private String orderCode;

    // ==================== 订单类型 ====================
    /**
     * 订单类型：1-医疗器械，2-非医疗器械，3-服务
     */
    private Integer orderType;

    /**
     * 业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）
     */
    private String businessType;

    // ==================== 机构信息 ====================
    /**
     * 提单机构ID
     */
    private Long orgId;

    /**
     * 提单机构名称（冗余）
     */
    private String orgName;

    /**
     * 操作员ID（创建人）
     */
    private Long operatorId;

    /**
     * 操作员姓名（冗余）
     */
    private String operatorName;

    /**
     * 操作员电话
     */
    private String operatorPhone;

    // ==================== 医院与科室 ====================
    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称（冗余）
     */
    private String hospitalName;

    /**
     * 科室ID
     */
    private Long deptId;

    /**
     * 科室名称（冗余）
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
    private String doctorPhone;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 患者年龄
     */
    private Integer patientAge;

    /**
     * 患者性别（字典 dict_code：12.1-男，12.2-女）
     */
    private String patientGender;

    // ==================== 业务信息 ====================
    /**
     * 是否加急：0-否，1-是
     */
    private Integer isUrgent;

    /**
     * 是否邮寄：0-否，1-是
     */
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

    /**
     * 设计开始时间
     */
    private LocalDateTime designStartTime;

    /**
     * 设计提交时间
     */
    private LocalDateTime designSubmitTime;

    /**
     * 用户确认时间
     */
    private LocalDateTime userConfirmTime;

    /**
     * 实际完成时间
     */
    private LocalDateTime actualCompleteTime;

    // ==================== 阶段 + 状态 ====================
    /**
     * 当前阶段：1-订单，2-设计，3-生产
     */
    private Integer phase;

    /**
     * 当前状态
     */
    private Integer status;

    // ==================== 当前处理人 ====================
    /**
     * 当前处理人ID
     */
    private Long currentHandlerId;

    /**
     * 当前处理人姓名
     */
    private String currentHandlerName;

    /**
     * 设计师ID
     */
    private Long designerId;

    /**
     * 生产员ID
     */
    private Long producerId;

    // ==================== 审核信息 ====================
    /**
     * 审核备注（驳回原因等）
     */
    private String auditRemark;

    /**
     * 设计审核备注
     */
    private String designReviewRemark;

    // ==================== 乐观锁 ====================
    /**
     * 版本号（乐观锁）
     */
    private Integer version;
}
