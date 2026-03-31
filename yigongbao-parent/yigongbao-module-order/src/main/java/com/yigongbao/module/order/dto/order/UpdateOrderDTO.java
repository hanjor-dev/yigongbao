package com.yigongbao.module.order.dto.order;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 更新订单 DTO
 * 用于订单信息的修改操作
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class UpdateOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 科室ID
     */
    private Long deptId;

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
}
