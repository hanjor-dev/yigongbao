package com.yigongbao.module.system.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.user.dto.ChangePasswordDTO;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO;
import com.yigongbao.module.system.user.dto.UserPageDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.vo.UserVO;

import java.util.List;

/**
 * 用户 Service 接口
 *
 * @author hanjor
 * @date 2026-03-17
 */
public interface UserService extends IService<UserEntity> {

    /**
     * 分页查询用户列表
     *
     * @param dto 分页查询参数
     * @return 分页后的用户列表
     */
    IPage<UserVO> listUser(UserPageDTO dto);

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情VO
     */
    UserVO getUserById(Long id);

    /**
     * 创建用户
     *
     * @param dto 创建参数
     */
    void createUser(CreateUserDTO dto);

    /**
     * 更新用户信息
     *
     * @param id  用户ID
     * @param dto 更新参数
     */
    void updateUser(Long id, UpdateUserDTO dto);

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户ID
     */
    void removeUser(Long id);

    /**
     * 修改用户状态
     *
     * @param id     用户ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 重置密码，将用户密码重置为系统默认密码
     *
     * @param userId 用户ID
     */
    void resetPassword(Long userId);

    /**
     * 修改密码
     *
     * @param id  用户ID
     * @param dto 密码修改参数（包含旧密码和新密码）
     */
    void changePassword(Long id, ChangePasswordDTO dto);

    /**
     * 用户自更新（仅允许修改手机号和头像）
     *
     * @param id  用户ID
     * @param dto 更新参数（仅手机号和头像）
     */
    void updateUserBySelf(Long id, UpdateUserBySelfDTO dto);

    /**
     * 统计指定部门下的用户数量
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    long countByDeptId(Long deptId);

    /**
     * 根据部门ID查询用户ID列表，用于设计工单数据权限过滤（DEPT 类型）
     *
     * @param deptId 部门ID
     * @return 该部门下所有正常状态用户的ID列表，deptId 为 null 时返回空列表
     */
    List<Long> listUserIdsByDeptId(Long deptId);

    /**
     * 根据机构ID查询用户ID列表，用于设计工单数据权限过滤（ORG 类型）
     *
     * @param orgId 机构ID
     * @return 该机构下所有正常状态用户的ID列表，orgId 为 null 时返回空列表
     */
    List<Long> listUserIdsByOrgId(Long orgId);
}
