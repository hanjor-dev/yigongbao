package com.yigongbao.module.system.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.role.dto.CreateRoleDTO;
import com.yigongbao.module.system.role.dto.UpdateRoleDTO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.vo.RoleVO;

/**
 * 角色 Service 接口
 *
 * @author hanjor
 * @date 2026-03-17
 */
public interface RoleService extends IService<RoleEntity> {

    /**
     * 分页查询角色列表
     *
     * @param pageNum     页码
     * @param pageSize   每页条数
     * @param roleName   角色名称（模糊查询）
     * @param accountType 账户分类（1=内部用户，2=外部用户）
     * @param status     状态
     * @return 分页后的角色列表
     */
    IPage<RoleVO> listRole(Integer pageNum, Integer pageSize, String roleName, Integer accountType, Integer status);

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    RoleVO getRoleById(Long id);

    /**
     * 创建角色
     *
     * @param dto 创建参数
     */
    void createRole(CreateRoleDTO dto);

    /**
     * 更新角色
     *
     * @param id  角色ID
     * @param dto 更新参数
     */
    void updateRole(Long id, UpdateRoleDTO dto);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void removeRole(Long id);

    /**
     * 修改角色状态
     *
     * @param id     角色ID
     * @param status 状态（0=禁用，1=正常）
     */
    void updateStatus(Long id, Integer status);
}
