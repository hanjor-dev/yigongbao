package com.yigongbao.module.system.resource.dto;

import lombok.Data;

/**
 * 资源分页查询 DTO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class ResourcePageDTO {

    /**
     * 资源名称（模糊查询）
     */
    private String resourceName;

    /**
     * 资源编码（模糊查询）
     */
    private String resourceCode;

    /**
     * 资源类型
     */
    private Integer resourceType;

    /**
     * 父级ID（精确筛选）
     */
    private Long parentId;

    /**
     * 查询范围：1=全部, 2=只查菜单(含一二级), 3=只查按钮
     */
    private Integer queryScope;

    /**
     * 状态
     */
    private Integer status;
}
