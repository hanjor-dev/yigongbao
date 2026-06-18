package com.yigongbao.module.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户只读查询 Mapper（通知模块内部使用，映射 sys_user 表，避免跨模块 Service 依赖）
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Mapper
public interface UserQueryMapper {

    @Select("SELECT data_scope_type FROM sys_role WHERE role_code = #{roleCode} AND is_deleted = 0 LIMIT 1")
    String findDataScopeTypeByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT id FROM sys_user WHERE role_code = #{roleCode} AND is_deleted = 0")
    List<Long> findUserIdsByRoleAll(@Param("roleCode") String roleCode);

    @Select("SELECT id FROM sys_user WHERE role_code = #{roleCode} AND org_id = #{orgId} AND is_deleted = 0")
    List<Long> findUserIdsByRoleAndOrg(@Param("roleCode") String roleCode, @Param("orgId") Long orgId);

    @Select("SELECT id FROM sys_user WHERE role_code = #{roleCode} AND dept_id = #{deptId} AND is_deleted = 0")
    List<Long> findUserIdsByRoleAndDept(@Param("roleCode") String roleCode, @Param("deptId") Long deptId);

    @Select("SELECT id FROM sys_user WHERE role_code = #{roleCode} AND center_id = #{centerId} AND is_deleted = 0")
    List<Long> findUserIdsByRoleAndCenter(@Param("roleCode") String roleCode, @Param("centerId") Long centerId);

    @Select("SELECT u.id FROM sys_user u JOIN sys_user_hospital uh ON uh.user_id = u.id " +
            "WHERE u.role_code = #{roleCode} AND uh.hospital_id = #{hospitalId} AND u.is_deleted = 0")
    List<Long> findUserIdsByRoleAndHospital(@Param("roleCode") String roleCode, @Param("hospitalId") Long hospitalId);
}
