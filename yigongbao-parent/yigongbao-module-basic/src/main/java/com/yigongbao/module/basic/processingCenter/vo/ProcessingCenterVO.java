package com.yigongbao.module.basic.processingCenter.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProcessingCenterVO {
    private Long id;
    private String centerCode;
    private String centerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
