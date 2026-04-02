package com.yigongbao.module.system.role.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class RolePageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 角色名称（模糊查询）
     */
    private String roleName;

    /**
     * 账户分类（1=内部用户，2=外部用户）
     */
    private Integer accountType;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;
}
