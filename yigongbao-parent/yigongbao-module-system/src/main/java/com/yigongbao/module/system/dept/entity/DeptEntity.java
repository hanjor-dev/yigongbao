package com.yigongbao.module.system.dept.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 部门 Entity
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
@TableName("sys_dept")
@EqualsAndHashCode(callSuper = false)
public class DeptEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码（系统唯一）
     */
    private String deptCode;

    /**
     * 部门类型（1=内部，2=外部）
     */
    private Integer deptType;

    /**
     * 部门负责人姓名（自由输入）
     */
    private String leaderUser;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
