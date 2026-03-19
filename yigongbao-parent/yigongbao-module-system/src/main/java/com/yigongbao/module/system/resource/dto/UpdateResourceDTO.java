package com.yigongbao.module.system.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新资源 DTO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class UpdateResourceDTO {

    /**
     * 父级ID（0=根节点/一级菜单）
     */
    @NotNull(message = "父级ID不能为空")
    private Long parentId;

    /**
     * 资源名称
     */
    @NotBlank(message = "资源名称不能为空")
    private String resourceName;

    /**
     * 资源编码（唯一标识，如：system:org、system:org:add）
     */
    @NotBlank(message = "资源编码不能为空")
    private String resourceCode;

    /**
     * 资源类型（1=一级菜单，2=二级菜单，3=按钮）
     */
    @NotNull(message = "资源类型不能为空")
    private Integer resourceType;

    /**
     * 菜单图标（一级/二级菜单）
     */
    private String icon;

    /**
     * 路由路径（一级/二级菜单）
     */
    private String path;

    /**
     * 组件路径（一级/二级菜单）
     */
    private String component;

    /**
     * 重定向路径（一级菜单可选）
     */
    private String redirect;

    /**
     * 排序（同级内升序）
     */
    private Integer sort;

    /**
     * 显示状态（0=隐藏，1=显示）
     */
    private Integer visible;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
