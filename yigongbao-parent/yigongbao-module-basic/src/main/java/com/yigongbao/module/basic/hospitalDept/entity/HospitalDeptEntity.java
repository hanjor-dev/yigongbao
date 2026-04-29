package com.yigongbao.module.basic.hospitalDept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 医院科室 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("hospital_dept")
@EqualsAndHashCode(callSuper = false)
public class HospitalDeptEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科室编码（如：HDEPT-0001）
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
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
