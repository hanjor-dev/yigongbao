package com.yigongbao.module.system.role.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
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
     * 账户分类名称
     */
    private String accountTypeName;

    /**
     * 是否启用医院范围权限（0=否，1=是）
     */
    private Integer hospitalScopeEnabled;

    /**
     * 是否启用医院范围权限名称
     */
    private String hospitalScopeEnabledName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 已分配的资源ID列表
     */
    private List<Long> resourceIds;
}
