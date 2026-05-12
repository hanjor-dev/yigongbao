package com.yigongbao.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
     * 查询角色下所有用户ID
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    @Select("SELECT id FROM sys_user WHERE role_id = #{roleId} AND is_deleted = 0")
    List<Long> selectIdsByRoleId(Long roleId);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    UserEntity selectByUsername(String username);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND is_deleted = 0 LIMIT 1")
    UserEntity selectByPhone(String phone);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    UserEntity selectByEmail(String email);

    /**
     *
     * @param orgId 机构ID
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE org_id = #{orgId} AND is_deleted = 0")
    Long countByOrgId(Long orgId);

    /**
     * 查询符合专业方向的设计师候选列表（按当前在手工单数升序）
     * 用于自动分配：取负载最低的第一位
     *
     * @param specialty 项目专业方向（单值，如 "7.1"）
     * @return 设计师列表，已按 current_load ASC 排序
     */
    @Select("""
        SELECT u.*,
               (SELECT COUNT(*) FROM order_main om
                WHERE om.designer_id = u.id
                  AND om.status BETWEEN 21 AND 29
                  AND om.is_deleted = 0) AS current_load
        FROM sys_user u
        WHERE u.role_code IN ('designer', 'designer-manager')
          AND u.status = 1
          AND u.is_deleted = 0
          AND FIND_IN_SET(#{specialty}, u.specialty) > 0
        ORDER BY current_load ASC
        """)
    List<UserEntity> selectAvailableDesigners(@Param("specialty") String specialty);

    /**
     * 查询符合任意一个专业方向的设计师列表（用于手动分配时的候选展示）
     * 注意：specialtyCondition 由 Service 层使用严格正则校验后拼接，防止注入
     *
     * @param specialtyCondition 已校验的 FIND_IN_SET 条件串，如
     *        "FIND_IN_SET('7.1', specialty) > 0 OR FIND_IN_SET('7.2', specialty) > 0"
     * @return 设计师列表，已按 current_load ASC 排序
     */
    @Select("""
        SELECT u.*,
               (SELECT COUNT(*) FROM order_main om
                WHERE om.designer_id = u.id
                  AND om.status BETWEEN 21 AND 29
                  AND om.is_deleted = 0) AS current_load
        FROM sys_user u
        WHERE u.role_code IN ('designer', 'designer-manager')
          AND u.status = 1
          AND u.is_deleted = 0
          AND (${specialtyCondition})
        ORDER BY current_load ASC
        """)
    List<UserEntity> selectDesignersBySpecialties(
            @Param("specialtyCondition") String specialtyCondition);
}
