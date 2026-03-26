package com.yigongbao.module.system.user.service.impl;

/**
 * 用户管理 Service 实现类
 * 处理用户相关的业务逻辑，包括用户CRUD、密码管理、状态管理等
 *
 * @author hanjor
 * @date 2026-03-19
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.convert.UserConvert;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户 Service 实现类
 * 处理用户相关的业务逻辑，包括用户CRUD、密码管理等
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private final OrgService orgService;
    private final DeptService deptService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final UserHospitalService userHospitalService;

    /**
     * 分页查询用户列表
     */
    @Override
    public IPage<UserVO> listUser(Integer pageNum, Integer pageSize, String username, String realName,
                                   Long orgId, Long deptId, Integer accountType, Integer status) {
        log.info("分页查询用户列表，pageNum={}, pageSize={}, username={}, realName={}, orgId={}, deptId={}, accountType={}, status={}",
                pageNum, pageSize, username, realName, orgId, deptId, accountType, status);
        try {
            Page<UserEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(username), UserEntity::getUsername, username)
                    .like(StrUtil.isNotBlank(realName), UserEntity::getRealName, realName)
                    .eq(Objects.nonNull(orgId), UserEntity::getOrgId, orgId)
                    .eq(Objects.nonNull(deptId), UserEntity::getDeptId, deptId)
                    .eq(Objects.nonNull(accountType), UserEntity::getAccountType, accountType)
                    .eq(Objects.nonNull(status), UserEntity::getStatus, status)
                    .orderByDesc(UserEntity::getCreateTime);
            IPage<UserEntity> pageResult = page(page, wrapper);

            // 批量查询关联数据，避免 N+1 问题
            Map<Long, List<Long>> userHospitalMap = Collections.emptyMap();
            List<UserEntity> records = pageResult.getRecords();
            if (!records.isEmpty()) {
                // 收集所有需要查询的 ID
                Set<Long> roleIds = records.stream()
                        .map(UserEntity::getRoleId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Set<Long> orgIds = records.stream()
                        .map(UserEntity::getOrgId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Set<Long> deptIds = records.stream()
                        .map(UserEntity::getDeptId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // 批量查询角色信息
                Map<Long, RoleEntity> roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                        : roleService.listByIds(roleIds).stream()
                                .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
                // 批量查询机构信息
                Map<Long, OrgEntity> orgMap = orgIds.isEmpty() ? Collections.emptyMap()
                        : orgService.listByIds(orgIds).stream()
                                .collect(Collectors.toMap(OrgEntity::getId, Function.identity()));
                // 批量查询部门信息
                Map<Long, DeptEntity> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                        : deptService.listByIds(deptIds).stream()
                                .collect(Collectors.toMap(DeptEntity::getId, Function.identity()));

                // 批量查询用户医院关联
                userHospitalMap = userHospitalService.listHospitalIdsByUserIds(
                        records.stream().map(UserEntity::getId).filter(Objects::nonNull).collect(Collectors.toList()));

                // 填充关联数据到 VO
                for (UserEntity entity : records) {
                    fillEntityWithNames(entity, roleMap, orgMap, deptMap);
                }
            }

            IPage<UserVO> voPage = pageResult.convert(UserConvert::toVO);
            // 转换后再填充 VO 中需要名称的字段和医院ID列表
            for (UserVO vo : voPage.getRecords()) {
                if (vo.getStatus() != null) {
                    vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
                }
                if (vo.getSex() != null) {
                    vo.setSexName(StatusConstants.getSexName(vo.getSex()));
                }
                if (vo.getAccountType() != null) {
                    vo.setAccountTypeName(StatusConstants.getAccountTypeName(vo.getAccountType()));
                }
                // 填充医院ID列表
                if (vo.getId() != null) {
                    vo.setHospitalIds(userHospitalMap.getOrDefault(vo.getId(), Collections.emptyList()));
                }
            }
            log.info("分页查询用户列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询用户列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询用户详情
     */
    @Override
    public UserVO getUserById(Long id) {
        log.info("根据ID查询用户详情，id={}", id);
        try {
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            UserVO vo = toVOWithNames(entity);
            log.info("查询用户详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询用户详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 填充实体关联名称（使用批量查询的 Map 数据）
     *
     * @param entity 用户实体
     * @param roleMap 角色Map
     * @param orgMap 机构Map
     * @param deptMap 部门Map
     */
    private void fillEntityWithNames(UserEntity entity, Map<Long, RoleEntity> roleMap,
                                    Map<Long, OrgEntity> orgMap, Map<Long, DeptEntity> deptMap) {
        if (entity == null) {
            return;
        }
        // 填充机构名称
        if (entity.getOrgId() != null) {
            OrgEntity orgEntity = orgMap.get(entity.getOrgId());
            if (orgEntity != null) {
                entity.setOrgName(orgEntity.getOrgName());
            }
        }
        // 填充部门名称
        if (entity.getDeptId() != null) {
            DeptEntity deptEntity = deptMap.get(entity.getDeptId());
            if (deptEntity != null) {
                entity.setDeptName(deptEntity.getDeptName());
            }
        }
        // 填充角色名称和编码
        if (entity.getRoleId() != null) {
            RoleEntity roleEntity = roleMap.get(entity.getRoleId());
            if (roleEntity != null) {
                entity.setRoleName(roleEntity.getRoleName());
                entity.setRoleCode(roleEntity.getRoleCode());
            }
        }
    }

    /**
     * 创建用户
     * 配置键：default.password
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(CreateUserDTO dto) {
        log.info("创建用户，username={}, orgId={}", dto.getUsername(), dto.getOrgId());
        try {
            // 校验用户名是否已存在
            if (isUsernameExists(dto.getUsername())) {
                log.warn("用户名已存在，username={}", dto.getUsername());
                throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
            }
            // 校验手机号是否已存在
            if (isPhoneExists(dto.getPhone())) {
                log.warn("手机号已存在，phone={}", dto.getPhone());
                throw new BusinessException(ErrorCodeEnum.USER_PHONE_EXISTS);
            }
            // 校验所属机构是否存在，并获取名称设置到冗余字段
            if (dto.getOrgId() != null) {
                OrgEntity orgEntity = orgService.getById(dto.getOrgId());
                if (orgEntity == null) {
                    log.warn("所属机构不存在，orgId={}", dto.getOrgId());
                    throw new BusinessException(ErrorCodeEnum.USER_ORG_NOT_FOUND);
                }
            }
            // 校验所属部门是否存在
            if (dto.getDeptId() != null) {
                DeptEntity deptEntity = deptService.getById(dto.getDeptId());
                if (deptEntity == null) {
                    log.warn("所属部门不存在，deptId={}", dto.getDeptId());
                    throw new BusinessException(ErrorCodeEnum.USER_DEPT_NOT_FOUND);
                }
            }
            // 校验角色是否存在
            if (dto.getRoleId() != null) {
                RoleEntity roleEntity = roleService.getById(dto.getRoleId());
                if (roleEntity == null) {
                    log.warn("角色不存在，roleId={}", dto.getRoleId());
                    throw new BusinessException(ErrorCodeEnum.USER_ROLE_NOT_FOUND);
                }
            }
            // DTO转换为实体对象
            UserEntity entity = UserConvert.toEntity(dto);
            // 填充冗余字段（通过id查询获取名称）
            fillRedundantFields(entity);
            // 密码加密存储（如果未提供密码则从系统配置获取默认密码）
            String rawPassword;
            if (StrUtil.isNotBlank(dto.getPassword())) {
                rawPassword = dto.getPassword();
            } else {
                // 从系统配置获取默认密码（已内置兜底逻辑，不会返回 null）
                rawPassword = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
                log.info("使用系统配置默认密码，configKey={}", SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
            }
            entity.setPassword(passwordEncoder.encode(rawPassword));
            entity.setStatus(StatusConstants.NORMAL);
            // 插入数据库
            save(entity);

            // 处理医院范围权限分配（改为依赖角色的 hospitalScopeEnabled）
            if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                if (dto.getRoleId() != null) {
                    RoleEntity role = roleService.getById(dto.getRoleId());
                    if (role != null && role.getHospitalScopeEnabled() != null && role.getHospitalScopeEnabled() == StatusConstants.YES) {
                        userHospitalService.assignHospitals(entity.getId(), dto.getHospitalIds());
                    }
                }
            }

            log.info("创建用户成功，id={}, username={}", entity.getId(), dto.getUsername());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建用户异常，username={}", dto.getUsername(), e);
            throw e;
        }
    }

    /**
     * 更新用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UpdateUserDTO dto) {
        log.info("更新用户，id={}", id);
        try {
            // 校验用户是否存在
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            // 校验手机号是否与其他用户重复
            if (StrUtil.isNotBlank(dto.getPhone()) && !dto.getPhone().equals(entity.getPhone())) {
                if (isPhoneExistsExcludingId(dto.getPhone(), id)) {
                    log.warn("手机号已存在，phone={}", dto.getPhone());
                    throw new BusinessException(ErrorCodeEnum.USER_PHONE_EXISTS);
                }
            }
            // 校验所属机构是否存在
            if (dto.getOrgId() != null && !dto.getOrgId().equals(entity.getOrgId())) {
                OrgEntity orgEntity = orgService.getById(dto.getOrgId());
                if (orgEntity == null) {
                    log.warn("所属机构不存在，orgId={}", dto.getOrgId());
                    throw new BusinessException(ErrorCodeEnum.USER_ORG_NOT_FOUND);
                }
                // 更新机构名称冗余字段
                entity.setOrgName(orgEntity.getOrgName());
            }
            // 校验所属部门是否存在
            if (dto.getDeptId() != null && !dto.getDeptId().equals(entity.getDeptId())) {
                DeptEntity deptEntity = deptService.getById(dto.getDeptId());
                if (deptEntity == null) {
                    log.warn("所属部门不存在，deptId={}", dto.getDeptId());
                    throw new BusinessException(ErrorCodeEnum.USER_DEPT_NOT_FOUND);
                }
                // 更新部门名称冗余字段
                entity.setDeptName(deptEntity.getDeptName());
            }
            // 校验角色是否存在
            if (dto.getRoleId() != null && !dto.getRoleId().equals(entity.getRoleId())) {
                RoleEntity roleEntity = roleService.getById(dto.getRoleId());
                if (roleEntity == null) {
                    log.warn("角色不存在，roleId={}", dto.getRoleId());
                    throw new BusinessException(ErrorCodeEnum.USER_ROLE_NOT_FOUND);
                }
                // 更新角色名称和编码冗余字段
                entity.setRoleName(roleEntity.getRoleName());
                entity.setRoleCode(roleEntity.getRoleCode());

                // 处理医院范围权限变更（覆盖式，改为依赖角色的 hospitalScopeEnabled）
                if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                    if (roleEntity.getHospitalScopeEnabled() != null && roleEntity.getHospitalScopeEnabled() == StatusConstants.YES) {
                        userHospitalService.assignHospitals(id, dto.getHospitalIds());
                    }
                }
            } else if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                // 角色未变更，但医院列表有变更（可能是编辑页单独调整医院）
                if (entity.getRoleId() != null) {
                    RoleEntity currentRole = roleService.getById(entity.getRoleId());
                    if (currentRole != null && currentRole.getHospitalScopeEnabled() != null && currentRole.getHospitalScopeEnabled() == StatusConstants.YES) {
                        userHospitalService.assignHospitals(id, dto.getHospitalIds());
                    }
                }
            }

            // 更新用户信息
            BeanUtils.copyProperties(dto, entity, "id", "username", "password", "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新用户成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新用户异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUser(Long id) {
        log.info("删除用户，id={}", id);
        try {
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            // 删除用户前先清理医院关联
            userHospitalService.assignHospitals(id, List.of());
            removeById(id);
            log.info("删除用户成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除用户异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改用户状态，id={}, status={}", id, status);
        try {
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改用户状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改用户状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    /**
     * 重置密码
     * 将用户密码重置为系统默认密码
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId) {
        log.info("重置密码，userId={}", userId);
        try {
            UserEntity entity = getById(userId);
            if (entity == null) {
                log.warn("用户不存在，userId={}", userId);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            // 从系统配置获取默认密码（已内置兜底逻辑，不会返回 null）
            String rawPassword = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
            entity.setPassword(passwordEncoder.encode(rawPassword));
            updateById(entity);
            log.info("重置密码成功，userId={}", userId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("重置密码异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String oldPassword, String newPassword) {
        log.info("修改密码，id={}", id);
        try {
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            // 校验旧密码是否正确
            if (!passwordEncoder.matches(oldPassword, entity.getPassword())) {
                log.warn("旧密码不正确，id={}", id);
                throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
            }
            // 更新密码
            entity.setPassword(passwordEncoder.encode(newPassword));
            updateById(entity);
            log.info("修改密码成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改密码异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 用户自更新（仅允许修改手机号和头像）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserBySelf(Long id, com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO dto) {
        log.info("用户自更新信息，id={}", id);
        try {
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            // 校验手机号是否与其他用户重复
            if (dto.getPhone() != null && !dto.getPhone().equals(entity.getPhone())) {
                if (isPhoneExistsExcludingId(dto.getPhone(), id)) {
                    log.warn("手机号已存在，phone={}", dto.getPhone());
                    throw new BusinessException(ErrorCodeEnum.USER_PHONE_EXISTS);
                }
            }
            // 只更新手机号和头像
            if (dto.getPhone() != null) {
                entity.setPhone(dto.getPhone());
            }
            if (dto.getAvatar() != null) {
                entity.setAvatar(dto.getAvatar());
            }
            updateById(entity);
            log.info("用户自更新成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户自更新异常，id={}", id, e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 填充冗余字段（通过ID查询获取名称）
     *
     * @param entity 用户实体
     */
    private void fillRedundantFields(UserEntity entity) {
        // 填充机构名称
        if (entity.getOrgId() != null) {
            OrgEntity orgEntity = orgService.getById(entity.getOrgId());
            if (orgEntity != null) {
                entity.setOrgName(orgEntity.getOrgName());
            }
        }
        // 填充部门名称
        if (entity.getDeptId() != null) {
            DeptEntity deptEntity = deptService.getById(entity.getDeptId());
            if (deptEntity != null) {
                entity.setDeptName(deptEntity.getDeptName());
            }
        }
        // 填充角色名称和编码
        if (entity.getRoleId() != null) {
            RoleEntity roleEntity = roleService.getById(entity.getRoleId());
            if (roleEntity != null) {
                entity.setRoleName(roleEntity.getRoleName());
                entity.setRoleCode(roleEntity.getRoleCode());
            }
        }
    }

    /**
     * 转换为VO并填充关联名称
     */
    private UserVO toVOWithNames(UserEntity entity) {
        UserVO vo = UserConvert.toVO(entity);
        if (vo == null) {
            return null;
        }
        // 状态名称
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        // 性别名称
        if (vo.getSex() != null) {
            vo.setSexName(StatusConstants.getSexName(vo.getSex()));
        }
        // 账户分类名称
        if (vo.getAccountType() != null) {
            vo.setAccountTypeName(StatusConstants.getAccountTypeName(vo.getAccountType()));
        }
        // 填充角色的 hospitalScopeEnabled
        if (vo.getRoleId() != null) {
            RoleEntity roleEntity = roleService.getById(vo.getRoleId());
            if (roleEntity != null) {
                vo.setHospitalScopeEnabled(roleEntity.getHospitalScopeEnabled());
            }
        }
        // 填充用户已分配的医院ID列表
        vo.setHospitalIds(userHospitalService.getHospitalIdsByUserId(vo.getId()));
        return vo;
    }

    /**
     * 校验用户名是否存在
     */
    private boolean isUsernameExists(String username) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)) > 0;
    }

    /**
     * 校验手机号是否存在
     */
    private boolean isPhoneExists(String phone) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getPhone, phone)) > 0;
    }

    /**
     * 校验手机号是否存在（排除指定ID）
     */
    private boolean isPhoneExistsExcludingId(String phone, Long excludeId) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getPhone, phone)
                .ne(UserEntity::getId, excludeId)) > 0;
    }
}
