package com.yigongbao.module.order.dto.order;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 更新订单 DTO
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class UpdateOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long hospitalId;
    private Long deptId;
    private Long doctorId;
    private String doctorPhone;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private Integer isUrgent;
    private Integer isPostal;
    private String postalAddress;
    private LocalDateTime expectedDeliveryDate;
}
