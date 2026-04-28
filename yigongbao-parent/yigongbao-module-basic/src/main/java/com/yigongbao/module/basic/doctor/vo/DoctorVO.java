package com.yigongbao.module.basic.doctor.vo;

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

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 医生电话
     */
    private String doctorPhone;

    /**
     * 所属医院ID
     */
    private Long hospitalId;

    /**
     * 所属医院名称
     */
    private String hospitalName;

    /**
     * 创建该医生记录的业务员ID
     */
    private Long creatorId;

    /**
     * 关联订单数量
     */
    private Integer orderCount;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private Long createBy;
}
