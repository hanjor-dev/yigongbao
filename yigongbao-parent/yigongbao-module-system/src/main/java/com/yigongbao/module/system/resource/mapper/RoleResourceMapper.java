package com.yigongbao.module.system.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.resource.entity.RoleResourceEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 角色资源关联 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface RoleResourceMapper extends BaseMapper<RoleResourceEntity> {

    /**
     * 根据角色ID查询资源ID列表
     *
     * @param roleId 角色ID
     * @return 资源ID列表
     */
    @Select("SELECT resource_id FROM sys_role_resource WHERE role_id = #{roleId}")
    List<Long> selectResourceIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据资源ID查询角色ID列表
     *
     * @param resourceId 资源ID
     * @return 角色ID列表
     */
    @Select("SELECT role_id FROM sys_role_resource WHERE resource_id = #{resourceId}")
    List<Long> selectRoleIdsByResourceId(@Param("resourceId") Long resourceId);

    /**
     * 根据资源ID统计关联角色数量
     *
     * @param resourceId 资源ID
     * @return 关联角色数量
     */
    @Select("SELECT COUNT(*) FROM sys_role_resource WHERE resource_id = #{resourceId}")
    Long countByResourceId(@Param("resourceId") Long resourceId);

    /**
     * 根据角色ID删除关联关系
     *
     * @param roleId 角色ID
     */
    @Delete("DELETE FROM sys_role_resource WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据资源ID删除关联关系
     *
     * @param resourceId 资源ID
     */
    @Delete("DELETE FROM sys_role_resource WHERE resource_id = #{resourceId}")
    void deleteByResourceId(@Param("resourceId") Long resourceId);

    /**
     * 检查角色-资源关联是否存在
     *
     * @param roleId     角色ID
     * @param resourceId 资源ID
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM sys_role_resource WHERE role_id = #{roleId} AND resource_id = #{resourceId}")
    Long existsByRoleIdAndResourceId(@Param("roleId") Long roleId, @Param("resourceId") Long resourceId);

    /**
     * 批量插入角色资源关联
     *
     * @param relations 关联列表
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_resource (role_id, resource_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.roleId}, #{item.resourceId})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<RoleResourceEntity> relations);
}
