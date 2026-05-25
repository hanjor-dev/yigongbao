package com.yigongbao.module.basic.processingCenter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("processing_center")
public class ProcessingCenterEntity extends BaseEntity {
    private String centerCode;
    private String centerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String deviceIdRanges;
    private Integer status;
    private String remark;
}
