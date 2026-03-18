package com.yigongbao.module.system.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.ResetPasswordDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.vo.UserVO;

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
     * @param pageNum    页码
     * @param pageSize  每页条数
     * @param username  用户名（模糊查询）
     * @param realName  真实姓名（模糊查询）
     * @param orgId     所属机构ID
     * @param deptId    所属部门ID
     * @param accountType 账户分类
     * @param status    状态
     * @return 分页后的用户列表
     */
    IPage<UserVO> listUser(Integer pageNum, Integer pageSize, String username, String realName,
                            Long orgId, Long deptId, Integer accountType, Integer status);

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserVO getUserById(Long id);

    /**
     * 创建用户
     *
     * @param dto 创建参数
     */
    void createUser(CreateUserDTO dto);

    /**
     * 更新用户
     *
     * @param id  用户ID
     * @param dto 更新参数
     */
    void updateUser(Long id, UpdateUserDTO dto);

    /**
     * 删除用户
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
     * 重置密码
     *
     * @param id  用户ID
     * @param dto 重置密码参数
     */
    void resetPassword(Long id, ResetPasswordDTO dto);

    /**
     * 修改密码
     *
     * @param id          用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long id, String oldPassword, String newPassword);

    /**
     * 用户自更新（仅允许修改手机号和头像）
     *
     * @param id  用户ID
     * @param dto 更新参数（仅手机号和头像）
     */
    void updateUserBySelf(Long id, com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO dto);
}
