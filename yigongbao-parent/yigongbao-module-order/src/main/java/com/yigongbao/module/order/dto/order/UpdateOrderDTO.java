package com.yigongbao.module.order.dto.order;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 更新订单 DTO
 * 用于订单信息的修改操作
 *
 * 【重要】needsPhysicalDelivery 变更规则：
 * - 仅在订单阶段（phase=10）允许修改
 * - 允许 0/2→1 的变更（非实体交付→需要实体交付）
 * - 不允许 1→0/2 的变更（需要实体交付→非实体交付）
 * 业务校验逻辑在 OrderMainServiceImpl.updateOrder 中实现
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class UpdateOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医生ID
     */
    private Long doctorId;

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

    /**
     * 期望交付时间
     */
    private LocalDateTime expectedDeliveryDate;

    /**
     * 是否需要实体交付：0-不需要，1-需要，2-异地打印
     * 【变更规则】仅在订单阶段允许修改，仅允许 0→1，不允许 1→0
     */
    private Integer needsPhysicalDelivery;
}
