package com.yigongbao.module.system.resource.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.resource.dto.CreateResourceDTO;
import com.yigongbao.module.system.resource.dto.ResourcePageDTO;
import com.yigongbao.module.system.resource.dto.UpdateResourceDTO;
import com.yigongbao.module.system.resource.entity.ResourceEntity;
import com.yigongbao.module.system.resource.vo.ResourceVO;

import java.util.List;

/**
 * 资源 Service
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface ResourceService extends IService<ResourceEntity> {

    /**
     * 分页查询资源列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param dto      查询条件
     * @return 分页结果
     */
    IPage<ResourceVO> pageResources(Integer pageNum, Integer pageSize, ResourcePageDTO dto);

    /**
     * 根据ID查询资源详情
     *
     * @param id 资源ID
     * @return 资源详情
     */
    ResourceVO getResourceById(Long id);

    /**
     * 创建资源
     *
     * @param dto 创建参数
     */
    void createResource(CreateResourceDTO dto);

    /**
     * 更新资源
     *
     * @param id  资源ID
     * @param dto 更新参数
     */
    void updateResource(Long id, UpdateResourceDTO dto);

    /**
     * 删除资源
     *
     * @param id 资源ID
     */
    void deleteResource(Long id);

    /**
     * 获取资源树（所有资源，用于管理后台）
     *
     * @return 资源树
     */
    List<ResourceVO> getResourceTree();

    /**
     * 获取角色已分配的资源ID列表
     *
     * @param roleId 角色ID
     * @return 资源ID列表
     */
    List<Long> getResourceIdsByRoleId(Long roleId);

    /**
     * 分配角色资源
     *
     * @param roleId     角色ID
     * @param resourceIds 资源ID列表
     */
    void assignResources(Long roleId, List<Long> resourceIds);

    /**
     * 获取用户拥有的菜单树（登录后返回给前端）
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    List<ResourceVO> getUserMenuTree(Long userId);

    /**
     * 获取用户拥有的按钮权限列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 获取带分配状态的资源树（用于角色分配资源场景）
     *
     * @param roleId 角色ID（为null时返回全部，checked=false）
     * @return 资源树，每节点含 checked=true/false
     */
    List<ResourceVO> getResourceTreeWithChecked(Long roleId);
}
