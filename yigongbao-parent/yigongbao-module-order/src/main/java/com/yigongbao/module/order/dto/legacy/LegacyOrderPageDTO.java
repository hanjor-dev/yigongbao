package com.yigongbao.module.order.dto.legacy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LegacyOrderPageDTO implements Serializable {
    @Min(1) private Integer pageNum = 1;
    @Min(1) @Max(100) private Integer pageSize = 20;
    private String keyword;
    private String status;
    private String businessType;
    private String printRequirement;
    private String deptName;
    private String hospitalName;
    private String areaName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;
    private String sortField;
    private String sortOrder;
}
