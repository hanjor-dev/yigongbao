package com.yigongbao.module.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 经典案例视图对象VO
 */
@Data
public class ClassicCaseVO {

    private Long id;
    private String orderCode;
    private String patientName;
    private Integer patientAge;
    private String patientGender;
    private String hospitalName;
    private String bodyPartName;
    private String projectName;
    private LocalDateTime classicCaseTime;
    private String classicCaseRemark;
    private LocalDateTime createTime;
}
