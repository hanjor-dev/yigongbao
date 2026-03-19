package com.yigongbao.module.system.resource.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资源 Entity
 * 整合菜单和按钮权限，统一管理前端路由和后端接口权限
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("sys_resource")
@EqualsAndHashCode(callSuper = false)
public class ResourceEntity extends BaseEntity {

    /**
     * 父级ID（0=根节点/一级菜单）
     */
    private Long parentId;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 资源编码（唯一标识，如：system:org、system:org:add）
     */
    private String resourceCode;

    /**
     * 资源类型（1=一级菜单，2=二级菜单，3=按钮）
     */
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
