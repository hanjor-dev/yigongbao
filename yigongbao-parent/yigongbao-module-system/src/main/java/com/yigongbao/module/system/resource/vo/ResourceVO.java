package com.yigongbao.module.system.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资源 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class ResourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

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
     * 资源类型描述
     */
    private String resourceTypeName;

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
     * 显示状态名称
     */
    private String visibleName;

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
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否已分配（用于角色分配资源场景）
     */
    private Boolean checked;

    /**
     * 子资源列表（用于树形结构）
     */
    private List<ResourceVO> children;
}
