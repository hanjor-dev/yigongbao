package com.yigongbao.module.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.role.convert.RoleConvert;
import com.yigongbao.module.system.role.dto.CreateRoleDTO;
import com.yigongbao.module.system.role.dto.RolePageDTO;
import com.yigongbao.module.system.role.dto.UpdateRoleDTO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.mapper.RoleMapper;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.role.vo.RoleVO;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import com.yigongbao.module.system.resource.mapper.RoleResourceMapper;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色 Service 实现类
 * 处理角色相关的业务逻辑，包括角色CRUD、状态管理等
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RoleEntity> implements RoleService {

    private final UserMapper userMapper;
    private final DictService dictService;
    private final ResourceService resourceService;
    private final RoleResourceMapper roleResourceMapper;

    /**
     * 分页查询角色列表
     *
     * @param dto 分页查询参数
     * @return 分页后的角色列表
     */
    @Override
    public IPage<RoleVO> listRole(RolePageDTO dto) {
        log.info("分页查询角色列表，dto={}", dto);
        try {
            // 如果未传入分页参数，使用默认值
            int pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
            int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
            Page<RoleEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(dto.getRoleName()), RoleEntity::getRoleName, dto.getRoleName())
                    .eq(Objects.nonNull(dto.getAccountType()), RoleEntity::getAccountType, dto.getAccountType())
                    .eq(Objects.nonNull(dto.getStatus()), RoleEntity::getStatus, dto.getStatus())
                    .orderByDesc(RoleEntity::getCreateTime);
            IPage<RoleEntity> pageResult = page(page, wrapper);
            IPage<RoleVO> voPage = pageResult.convert(this::toVOWithNames);
            log.info("分页查询角色列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询角色列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @Override
    public RoleVO getRoleById(Long id) {
        log.info("根据ID查询角色详情，id={}", id);
        try {
            RoleEntity entity = getById(id);
            if (entity == null) {
                log.warn("角色不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            RoleVO vo = toVOWithNames(entity);
            // 填充已分配的资源ID列表
            vo.setResourceIds(resourceService.getResourceIdsByRoleId(id));
            log.info("查询角色详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询角色详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建角色
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(CreateRoleDTO dto) {
        log.info("创建角色，roleName={}, roleCode={}", dto.getRoleName(), dto.getRoleCode());
        try {
            // 校验角色编码是否已存在
            if (isRoleCodeExists(dto.getRoleCode())) {
                log.warn("角色编码已存在，roleCode={}", dto.getRoleCode());
                throw new BusinessException(ErrorCodeEnum.ROLE_EXISTS);
            }
            // DTO转换为实体对象
            RoleEntity entity = RoleConvert.toEntity(dto);
            entity.setStatus(StatusConstants.NORMAL);
            // 插入数据库
            save(entity);
            log.info("创建角色成功，id={}, roleCode={}", entity.getId(), dto.getRoleCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建角色异常，roleName={}", dto.getRoleName(), e);
            throw e;
        }
    }

    /**
     * 更新角色
     *
     * @param id  角色ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, UpdateRoleDTO dto) {
        log.info("更新角色，id={}", id);
        try {
            // 校验角色是否存在
            RoleEntity entity = getById(id);
            if (entity == null) {
                log.warn("角色不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 校验角色编码是否与其他角色重复
            if (StrUtil.isNotBlank(dto.getRoleCode()) && !dto.getRoleCode().equals(entity.getRoleCode())) {
                if (isRoleCodeExistsExcludingId(dto.getRoleCode(), id)) {
                    log.warn("角色编码已存在，roleCode={}", dto.getRoleCode());
                    throw new BusinessException(ErrorCodeEnum.ROLE_EXISTS);
                }
            }
            // 更新角色信息（先记录旧名称，用于后续同步冗余字段）
            String oldRoleName = entity.getRoleName();
            BeanUtils.copyProperties(dto, entity, "id", "roleCode", "createTime", "updateTime", "createBy", "updateBy");
            // 更新数据库
            updateById(entity);
            // 角色名称变更时，同步更新 sys_user 中的冗余字段 role_name
            if (StrUtil.isNotBlank(dto.getRoleName()) && !dto.getRoleName().equals(oldRoleName)) {
                userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserEntity>()
                        .eq(UserEntity::getRoleId, id)
                        .set(UserEntity::getRoleName, dto.getRoleName()));
            }
            log.info("更新角色成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新角色异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long id) {
        log.info("删除角色，id={}", id);
        try {
            // 校验角色是否存在
            RoleEntity entity = getById(id);
            if (entity == null) {
                log.warn("角色不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 校验该角色下是否有用户
            if (hasUsers(id)) {
                log.warn("该角色下存在用户，无法删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ROLE_HAS_USERS);
            }
            // 先清理角色资源关联表，再逻辑删除角色
            roleResourceMapper.deleteByRoleId(id);
            removeById(id);
            log.info("删除角色成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除角色异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改角色状态
     *
     * @param id     角色ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改角色状态，id={}, status={}", id, status);
        try {
            // 校验角色是否存在
            RoleEntity entity = getById(id);
            if (entity == null) {
                log.warn("角色不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 更新状态
            entity.setStatus(status);
            updateById(entity);
            log.info("修改角色状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改角色状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    /**
     * 全量查询角色列表（用于前端下拉选择）
     *
     * @return 角色列表（包含关联名称）
     */
    @Override
    public List<RoleVO> listAllRole() {
        log.info("全量查询角色列表，用于下拉选择");
        try {
            LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(RoleEntity::getRoleName);
            List<RoleEntity> entityList = list(wrapper);
            List<RoleVO> voList = entityList.stream()
                    .map(this::toVOWithNames)
                    .collect(Collectors.toList());
            log.info("全量查询角色列表成功，总数={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("全量查询角色列表异常", e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为VO并填充关联名称
     *
     * @param entity 角色实体
     * @return 角色VO
     */
    private RoleVO toVOWithNames(RoleEntity entity) {
        RoleVO vo = RoleConvert.toVO(entity);
        if (vo == null) {
            return null;
        }
        // 填充账户分类名称
        if (vo.getAccountType() != null) {
            DictVO dictVO = dictService.getByDictCode(vo.getAccountType());
            vo.setAccountTypeName(dictVO != null ? dictVO.getDictName() : "");
        }
        // 填充数据权限范围名称
        if (vo.getDataScopeType() != null) {
            vo.setDataScopeTypeName(DataScopeTypeEnum.getDescByCode(vo.getDataScopeType()));
        }
        // 填充状态名称
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        return vo;
    }

    /**
     * 校验角色编码是否存在
     *
     * @param roleCode 角色编码
     * @return true-存在，false-不存在
     */
    private boolean isRoleCodeExists(String roleCode) {
        return count(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getRoleCode, roleCode)) > 0;
    }

    /**
     * 校验角色编码是否存在（排除指定ID）
     *
     * @param roleCode  角色编码
     * @param excludeId 排除的角色ID
     * @return true-存在，false-不存在
     */
    private boolean isRoleCodeExistsExcludingId(String roleCode, Long excludeId) {
        return count(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getRoleCode, roleCode)
                .ne(RoleEntity::getId, excludeId)) > 0;
    }

    /**
     * 校验该角色下是否有用户
     *
     * @param roleId 角色ID
     * @return true-有用户，false-无用户
     */
    private boolean hasUsers(Long roleId) {
        return userMapper.countByRoleId(roleId) > 0;
    }
}
