package com.yigongbao.module.system.role.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 更新角色 DTO
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class UpdateRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String roleName;

    /**
     * 角色编码
     */
    @Size(max = 32, message = "角色编码长度不能超过32个字符")
    private String roleCode;

    /**
     * 角色描述
     */
    @Size(max = 256, message = "角色描述长度不能超过256个字符")
    private String roleDesc;

    /**
     * 账户分类（1=内部用户，2=外部用户）
     */
    private Integer accountType;

    /**
     * 是否启用医院范围权限（0=否，1=是）
     */
    private Integer hospitalScopeEnabled;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;
}
