package com.yigongbao.module.system.doctor.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医生 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class DoctorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String doctorName;
    private String doctorPhone;
    private Long hospitalId;
    private String hospitalName;
    private Long creatorId;
    private Integer orderCount;
    private Integer status;
    private String statusName;
    private String remark;
    private LocalDateTime createTime;
    private Long createBy;
}
