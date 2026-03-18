package com.yigongbao.module.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色 Entity
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
@TableName("sys_role")
@EqualsAndHashCode(callSuper = false)
public class RoleEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码（系统唯一）
     */
    private String roleCode;

    /**
     * 角色描述
     */
    private String roleDesc;

    /**
     * 账户分类（1=内部用户，2=外部用户）
     */
    private Integer accountType;

    /**
     * 数据范围（1=全部数据，2=本机构，3=仅自己，4=医院范围，5=部门范围）
     */
    private Integer dataScope;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
