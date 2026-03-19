package com.yigongbao.module.system.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.BaseEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.ResourceTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.resource.convert.ResourceConvert;
import com.yigongbao.module.system.resource.dto.CreateResourceDTO;
import com.yigongbao.module.system.resource.dto.ResourcePageDTO;
import com.yigongbao.module.system.resource.dto.UpdateResourceDTO;
import com.yigongbao.module.system.resource.entity.ResourceEntity;
import com.yigongbao.module.system.resource.entity.RoleResourceEntity;
import com.yigongbao.module.system.resource.mapper.ResourceMapper;
import com.yigongbao.module.system.resource.mapper.RoleResourceMapper;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.resource.vo.ResourceVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, ResourceEntity> implements ResourceService {

    private final RoleResourceMapper roleResourceMapper;
    private final UserMapper userMapper;

    /**
     * 分页查询资源列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param dto      查询条件
     * @return 分页后的资源列表
     */
    @Override
    public IPage<ResourceVO> pageResources(Integer pageNum, Integer pageSize, ResourcePageDTO dto) {
        log.info("分页查询资源列表，pageNum={}, pageSize={}, resourceName={}, resourceCode={}, resourceType={}, parentId={}, queryScope={}, status={}",
                pageNum, pageSize, dto.getResourceName(), dto.getResourceCode(), dto.getResourceType(), dto.getParentId(), dto.getQueryScope(), dto.getStatus());
        Page<ResourceEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(dto.getResourceName()), ResourceEntity::getResourceName, dto.getResourceName())
                .like(StringUtils.hasText(dto.getResourceCode()), ResourceEntity::getResourceCode, dto.getResourceCode())
                .eq(dto.getStatus() != null, ResourceEntity::getStatus, dto.getStatus());

        // queryScope 筛选
        if (dto.getQueryScope() != null) {
            if (dto.getQueryScope() == 2) {
                // 只查菜单（一级+二级）
                wrapper.in(ResourceEntity::getResourceType,
                        ResourceTypeEnum.MENU_FIRST.getCode(),
                        ResourceTypeEnum.MENU_SECOND.getCode());
            } else if (dto.getQueryScope() == 3) {
                // 只查按钮
                wrapper.eq(ResourceEntity::getResourceType, ResourceTypeEnum.BUTTON.getCode());
            }
            // queryScope=1 或未传：不做额外筛选
        } else if (dto.getResourceType() != null) {
            // resourceType 精确筛选（优先级低于 queryScope）
            wrapper.eq(ResourceEntity::getResourceType, dto.getResourceType());
        }

        // parentId 筛选
        if (dto.getParentId() != null) {
            wrapper.eq(ResourceEntity::getParentId, dto.getParentId());
        }

        wrapper.orderByAsc(ResourceEntity::getSort)
                .orderByDesc(BaseEntity::getCreateTime);

        IPage<ResourceEntity> pageResult = baseMapper.selectPage(page, wrapper);
        return pageResult.convert(ResourceConvert::toVO);
    }

    /**
     * 根据ID查询资源详情
     */
    @Override
    public ResourceVO getResourceById(Long id) {
        log.info("根据ID查询资源详情，id={}", id);
        ResourceEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            log.warn("资源不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }
        return ResourceConvert.toVO(entity);
    }

    /**
     * 创建资源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createResource(CreateResourceDTO dto) {
        log.info("创建资源，resourceCode={}", dto.getResourceCode());

        // 校验资源编码唯一性
        if (baseMapper.selectIdByCode(dto.getResourceCode()) != null) {
            log.warn("资源编码已存在，resourceCode={}", dto.getResourceCode());
            throw new BusinessException(ErrorCodeEnum.RESOURCE_EXISTS);
        }

        // 按钮类型不需要校验父级
        if (!ResourceTypeEnum.BUTTON.getCode().equals(dto.getResourceType())) {
            // 校验父级资源是否存在
            if (!dto.getParentId().equals(0L)) {
                ResourceEntity parent = baseMapper.selectById(dto.getParentId());
                if (parent == null) {
                    log.warn("父级资源不存在，parentId={}", dto.getParentId());
                    throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
                }
            }
        }

        ResourceEntity entity = ResourceConvert.toEntity(dto);
        baseMapper.insert(entity);
        log.info("创建资源成功，id={}", entity.getId());
    }

    /**
     * 更新资源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResource(Long id, UpdateResourceDTO dto) {
        log.info("更新资源，id={}", id);

        ResourceEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            log.warn("资源不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }

        // 校验资源编码唯一性（排除自身）
        Long existingId = baseMapper.selectIdByCode(dto.getResourceCode());
        if (existingId != null && !existingId.equals(id)) {
            log.warn("资源编码已存在，resourceCode={}", dto.getResourceCode());
            throw new BusinessException(ErrorCodeEnum.RESOURCE_EXISTS);
        }

        // 按钮类型不允许修改父级
        if (ResourceTypeEnum.BUTTON.getCode().equals(entity.getResourceType())) {
            dto.setParentId(entity.getParentId());
            dto.setResourceType(entity.getResourceType());
        }

        // 更新资源信息
        BeanUtils.copyProperties(dto, entity);
        baseMapper.updateById(entity);
        log.info("更新资源成功，id={}", id);
    }

    /**
     * 删除资源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long id) {
        log.info("删除资源，id={}", id);

        ResourceEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            log.warn("资源不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }

        // 校验是否有子资源
        Long childCount = baseMapper.countByParentId(id);
        if (childCount > 0) {
            log.warn("资源下存在子资源，无法删除，id={}", id);
            throw new BusinessException(ErrorCodeEnum.RESOURCE_HAS_CHILDREN);
        }

        // 校验是否有角色关联
        Long roleCount = roleResourceMapper.countByResourceId(id);
        if (roleCount > 0) {
            log.warn("资源已分配给角色，无法删除，id={}", id);
            throw new BusinessException(ErrorCodeEnum.RESOURCE_HAS_ROLES);
        }

        baseMapper.deleteById(id);
        log.info("删除资源成功，id={}", id);
    }

    /**
     * 获取资源树（所有资源，用于管理后台）
     */
    @Override
    public List<ResourceVO> getResourceTree() {
        log.info("获取资源树");
        LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceEntity::getStatus, StatusConstants.NORMAL)
                .orderByAsc(ResourceEntity::getSort);
        List<ResourceEntity> allResources = baseMapper.selectList(wrapper);
        return buildResourceTree(allResources, 0L);
    }

    /**
     * 递归构建资源树
     */
    private List<ResourceVO> buildResourceTree(List<ResourceEntity> allResources, Long parentId) {
        if (allResources == null || allResources.isEmpty()) {
            return new ArrayList<>();
        }
        return allResources.stream()
                .filter(r -> r.getParentId().equals(parentId))
                .filter(r -> r.getVisible().equals(StatusConstants.NORMAL))
                .map(ResourceConvert::toVO)
                .peek(vo -> vo.setChildren(buildResourceTree(allResources, vo.getId())))
                .sorted(Comparator.comparing(ResourceVO::getSort))
                .collect(Collectors.toList());
    }

    /**
     * 获取角色已分配的资源ID列表
     */
    @Override
    public List<Long> getResourceIdsByRoleId(Long roleId) {
        return roleResourceMapper.selectResourceIdsByRoleId(roleId);
    }

    /**
     * 分配角色资源
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignResources(Long roleId, List<Long> resourceIds) {
        log.info("分配角色资源，roleId={}, resourceIds={}", roleId, resourceIds);

        // 删除原有关联
        roleResourceMapper.deleteByRoleId(roleId);

        // 批量插入新关联（仅当非空时）
        if (resourceIds != null && !resourceIds.isEmpty()) {
            // 校验 resourceIds 有效性：只保留存在且未删除的资源
            LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ResourceEntity::getId, resourceIds)
                    .eq(ResourceEntity::getStatus, StatusConstants.NORMAL);
            List<ResourceEntity> validResources = baseMapper.selectList(wrapper);
            List<Long> validIds = validResources.stream()
                    .map(ResourceEntity::getId)
                    .collect(Collectors.toList());

            if (!validIds.isEmpty()) {
                List<RoleResourceEntity> relations = validIds.stream()
                        .map(resourceId -> {
                            RoleResourceEntity r = new RoleResourceEntity();
                            r.setRoleId(roleId);
                            r.setResourceId(resourceId);
                            return r;
                        })
                        .collect(Collectors.toList());

                // 批量插入
                roleResourceMapper.insertBatch(relations);
                log.info("分配角色资源成功，roleId={}, 传入={}个, 有效={}个",
                        roleId, resourceIds.size(), validIds.size());
            } else {
                log.info("分配角色资源成功，roleId={}, 传入的resourceIds均无效", roleId);
            }
        } else {
            log.info("分配角色资源成功，roleId={}, 已清空资源", roleId);
        }
    }

    /**
     * 获取用户拥有的菜单树（登录后返回给前端）
     */
    @Override
    public List<ResourceVO> getUserMenuTree(Long userId) {
        log.info("获取用户菜单树，userId={}", userId);

        // 查询用户信息
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return new ArrayList<>();
        }

        // 获取角色关联的资源ID列表
        List<Long> resourceIds = roleResourceMapper.selectResourceIdsByRoleId(user.getRoleId());
        if (resourceIds == null || resourceIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询用户拥有的菜单资源（一级和二级菜单）
        LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ResourceEntity::getId, resourceIds)
                .in(ResourceEntity::getResourceType,
                        ResourceTypeEnum.MENU_FIRST.getCode(),
                        ResourceTypeEnum.MENU_SECOND.getCode())
                .eq(ResourceEntity::getStatus, StatusConstants.NORMAL)
                .eq(ResourceEntity::getVisible, StatusConstants.NORMAL)
                .orderByAsc(ResourceEntity::getSort);

        List<ResourceEntity> userResources = baseMapper.selectList(wrapper);
        return buildResourceTree(userResources, 0L);
    }

    /**
     * 获取用户拥有的按钮权限列表
     */
    @Override
    public List<String> getUserPermissions(Long userId) {
        log.info("获取用户按钮权限，userId={}", userId);

        // 查询用户信息
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return new ArrayList<>();
        }

        // 获取角色关联的资源ID列表
        List<Long> resourceIds = roleResourceMapper.selectResourceIdsByRoleId(user.getRoleId());
        if (resourceIds == null || resourceIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询按钮权限
        LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ResourceEntity::getId, resourceIds)
                .eq(ResourceEntity::getResourceType, ResourceTypeEnum.BUTTON.getCode())
                .eq(ResourceEntity::getStatus, StatusConstants.NORMAL);

        List<ResourceEntity> buttonResources = baseMapper.selectList(wrapper);
        return buttonResources.stream()
                .map(ResourceEntity::getResourceCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取带分配状态的资源树（用于角色分配资源场景）
     */
    @Override
    public List<ResourceVO> getResourceTreeWithChecked(Long roleId) {
        log.info("获取带分配状态的资源树，roleId={}", roleId);

        // 查询所有正常状态资源
        LambdaQueryWrapper<ResourceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceEntity::getStatus, StatusConstants.NORMAL)
                .orderByAsc(ResourceEntity::getSort);
        List<ResourceEntity> allResources = baseMapper.selectList(wrapper);

        // 获取角色已分配的资源ID（如果 roleId 不为 null）
        List<Long> assignedIds = new ArrayList<>();
        if (roleId != null) {
            assignedIds = roleResourceMapper.selectResourceIdsByRoleId(roleId);
        }

        // 构建带 checked 状态的资源树
        List<ResourceVO> tree = buildResourceTreeWithChecked(allResources, 0L, assignedIds);
        return tree;
    }

    /**
     * 递归构建带 checked 状态的资源树
     */
    private List<ResourceVO> buildResourceTreeWithChecked(List<ResourceEntity> allResources, Long parentId, List<Long> assignedIds) {
        if (allResources == null || allResources.isEmpty()) {
            return new ArrayList<>();
        }
        return allResources.stream()
                .filter(r -> r.getParentId().equals(parentId))
                .filter(r -> r.getVisible().equals(StatusConstants.NORMAL))
                .map(entity -> {
                    ResourceVO vo = ResourceConvert.toVO(entity);
                    vo.setChecked(assignedIds.contains(entity.getId()));
                    vo.setChildren(buildResourceTreeWithChecked(allResources, vo.getId(), assignedIds));
                    return vo;
                })
                .sorted(Comparator.comparing(ResourceVO::getSort))
                .collect(Collectors.toList());
    }
}
