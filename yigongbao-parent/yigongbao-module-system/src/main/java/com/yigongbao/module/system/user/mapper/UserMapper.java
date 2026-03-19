package com.yigongbao.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 统计部门下的用户数量
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND is_deleted = 0")
    Long countByDeptId(Long deptId);

    /**
     * 统计角色下的用户数量
     *
     * @param roleId 角色ID
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE role_id = #{roleId} AND is_deleted = 0")
    Long countByRoleId(Long roleId);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    UserEntity selectByUsername(String username);

    /**
     * 统计机构下的用户数量
     *
     * @param orgId 机构ID
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE org_id = #{orgId} AND is_deleted = 0")
    Long countByOrgId(Long orgId);
}
