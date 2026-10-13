package com.yigongbao.module.basic.hospitalDept.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院科室 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class HospitalDeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 科室编码
     */
    private String hospitalDeptCode;

    /**
     * 科室名称
     */
    private String hospitalDeptName;

    /**
     * 排序
     */
    private Integer sort;

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
