package com.yigongbao.module.basic.doctor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 医生 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("doctor")
@EqualsAndHashCode(callSuper = false)
public class DoctorEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 医生电话
     */
    private String doctorPhone;

    /**
     * 所属医疗机构ID（关联sys_org表，orgType=1.3）
     */
    private Long hospitalId;

    /**
     * 创建该医生记录的业务员ID
     */
    private Long creatorId;

    /**
     * 关联订单数量
     */
    private Integer orderCount;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
