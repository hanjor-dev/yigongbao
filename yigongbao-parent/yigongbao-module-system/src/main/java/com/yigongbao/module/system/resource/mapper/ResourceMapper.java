package com.yigongbao.module.system.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.resource.entity.ResourceEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资源 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface ResourceMapper extends BaseMapper<ResourceEntity> {

    /**
     * 根据父级ID统计子资源数量
     *
     * @param parentId 父级ID
     * @return 子资源数量
     */
    @Select("SELECT COUNT(*) FROM sys_resource WHERE parent_id = #{parentId} AND is_deleted = 0")
    Long countByParentId(@Param("parentId") Long parentId);

    /**
     * 根据资源编码查询资源ID
     *
     * @param resourceCode 资源编码
     * @return 资源ID
     */
    @Select("SELECT id FROM sys_resource WHERE resource_code = #{resourceCode} AND is_deleted = 0 LIMIT 1")
    Long selectIdByCode(@Param("resourceCode") String resourceCode);
}
