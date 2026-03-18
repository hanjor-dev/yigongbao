package com.yigongbao.module.system.role.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 创建角色 DTO
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class CreateRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过64个字符")
    private String roleName;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
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
    @NotNull(message = "账户分类不能为空")
    private Integer accountType;

    /**
     * 数据范围（1=全部数据，2=本机构，3=仅自己，4=医院范围，5=部门范围）
     */
    private Integer dataScope;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;
}
