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
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.convert.UserConvert;
import com.yigongbao.module.system.user.dto.ChangePasswordDTO;
import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.dto.UserPageDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
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

    /** 需要填写专业方向的角色编码 */
    private static final List<String> SPECIALTY_REQUIRED_ROLES = List.of("designer", "designer-manager");

    private final OrgService orgService;
    private final DeptService deptService;
    private final RoleService roleService;
    private final DictService dictService;
    private final HospitalService hospitalService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final UserHospitalService userHospitalService;

    /**
     * 分页查询用户列表
     */
    @Override
    public IPage<UserVO> listUser(UserPageDTO dto) {
        log.info("分页查询用户列表，dto={}", dto);
        // 如果未传入分页参数，使用默认值
        int pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
        Page<UserEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getUsername()), UserEntity::getUsername, dto.getUsername())
                .like(StrUtil.isNotBlank(dto.getRealName()), UserEntity::getRealName, dto.getRealName())
                .eq(Objects.nonNull(dto.getOrgId()), UserEntity::getOrgId, dto.getOrgId())
                .eq(Objects.nonNull(dto.getDeptId()), UserEntity::getDeptId, dto.getDeptId())
                .eq(Objects.nonNull(dto.getAccountType()), UserEntity::getAccountType, dto.getAccountType())
                .eq(Objects.nonNull(dto.getStatus()), UserEntity::getStatus, dto.getStatus())
                .orderByDesc(UserEntity::getCreateTime);
        IPage<UserEntity> pageResult = page(page, wrapper);

        // 批量查询关联数据，避免 N+1 问题
        Map<Long, List<Long>> userHospitalMap = Collections.emptyMap();
        Map<Long, RoleEntity> roleMap = Collections.emptyMap();
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
            roleMap = roleIds.isEmpty() ? Collections.emptyMap()
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
            // 填充角色的 dataScopeType
            if (vo.getRoleId() != null) {
                RoleEntity roleEntity = roleMap.get(vo.getRoleId());
                if (roleEntity != null) {
                    vo.setDataScopeType(roleEntity.getDataScopeType());
                }
            }
            // 填充医院ID列表
            if (vo.getId() != null) {
                vo.setHospitalIds(userHospitalMap.getOrDefault(vo.getId(), Collections.emptyList()));
            }
        }
        log.info("分页查询用户列表成功，总数={}", pageResult.getTotal());
        return voPage;
    }

    /**
     * 根据ID查询用户详情
     */
    @Override
    public UserVO getUserById(Long id) {
        log.info("根据ID查询用户详情，id={}", id);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        UserVO vo = toVOWithNames(entity);
        log.info("查询用户详情成功，id={}", id);
        return vo;
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
        String maskedUsername = maskUsername(dto.getUsername());
        log.info("创建用户，username={}, orgId={}", maskedUsername, dto.getOrgId());
        try {
            // 校验用户名是否已存在
            if (isUsernameExists(dto.getUsername())) {
                log.warn("用户名已存在，username={}", maskedUsername);
                throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
            }
            // 校验手机号是否已存在
            if (isPhoneExists(dto.getPhone())) {
                log.warn("手机号已存在，phone={}", maskPhone(dto.getPhone()));
                throw new BusinessException(ErrorCodeEnum.USER_PHONE_EXISTS);
            }
            // 校验所属机构是否存在，并获取名称设置到冗余字段
            OrgEntity orgEntity = null;
            if (dto.getOrgId() != null) {
                orgEntity = orgService.getById(dto.getOrgId());
                if (orgEntity == null) {
                    log.warn("所属机构不存在，orgId={}", dto.getOrgId());
                    throw new BusinessException(ErrorCodeEnum.USER_ORG_NOT_FOUND);
                }
            }
            // 校验所属部门是否存在
            DeptEntity deptEntity = null;
            if (dto.getDeptId() != null) {
                deptEntity = deptService.getById(dto.getDeptId());
                if (deptEntity == null) {
                    log.warn("所属部门不存在，deptId={}", dto.getDeptId());
                    throw new BusinessException(ErrorCodeEnum.USER_DEPT_NOT_FOUND);
                }
            }
            // 校验角色是否存在
            RoleEntity roleEntity = null;
            if (dto.getRoleId() != null) {
                roleEntity = roleService.getById(dto.getRoleId());
                if (roleEntity == null) {
                    log.warn("角色不存在，roleId={}", dto.getRoleId());
                    throw new BusinessException(ErrorCodeEnum.USER_ROLE_NOT_FOUND);
                }
            }
            // accountType 为空时默认设置为内部用户（1）
            if (dto.getAccountType() == null) {
                dto.setAccountType(1);
                log.info("账户分类未指定，默认设置为内部用户");
            }
            // 角色业务规则校验（hospitals范围 + 设计师specialty）
            validateHospitalScope(roleEntity, dto.getHospitalIds());
            validateSpecialty(roleEntity, dto.getSpecialty());
            // DTO转换为实体对象
            UserEntity entity = UserConvert.toEntity(dto);
            // 填充冗余字段（复用已查询的实体，避免重复查询）
            fillRedundantFields(entity, orgEntity, deptEntity, roleEntity);
            // 密码加密存储（如果未提供密码则从系统配置获取默认密码）
            String rawPassword;
            if (StrUtil.isNotBlank(dto.getPassword())) {
                rawPassword = dto.getPassword();
                // 密码强度校验：必须包含字母和数字
                if (!isPasswordStrong(rawPassword)) {
                    log.warn("密码强度不足，username={}", maskedUsername);
                    throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_WEAK);
                }
            } else {
                // 从系统配置获取默认密码（已内置兜底逻辑，不会返回 null）
                rawPassword = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
                log.info("使用系统配置默认密码，configKey={}", SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
            }
            entity.setPassword(passwordEncoder.encode(rawPassword));
            entity.setStatus(StatusConstants.NORMAL);
            // 插入数据库
            save(entity);

            // 处理医院范围权限分配（当角色的 dataScopeType=hospitals 时才分配）
            if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                if (roleEntity != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(roleEntity.getDataScopeType())) {
                    userHospitalService.assignHospitals(entity.getId(), dto.getHospitalIds());
                }
            }

            log.info("创建用户成功，id={}, username={}", entity.getId(), maskedUsername);
        } catch (BusinessException e) {
            throw e;
        } catch (DuplicateKeyException e) {
            log.warn("用户名或手机号冲突（并发），username={}", maskedUsername, e);
            throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
        } catch (Exception e) {
            log.error("创建用户异常，username={}", maskedUsername, e);
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
                    log.warn("手机号已存在，phone={}", maskPhone(dto.getPhone()));
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
            // 角色校验：先查存在性，再做业务规则校验，复用同一次查询结果
            RoleEntity newRole = null;
            if (dto.getRoleId() != null && !dto.getRoleId().equals(entity.getRoleId())) {
                newRole = roleService.getById(dto.getRoleId());
                if (newRole == null) {
                    log.warn("角色不存在，roleId={}", dto.getRoleId());
                    throw new BusinessException(ErrorCodeEnum.USER_ROLE_NOT_FOUND);
                }
            }
            // 确定本次操作生效的角色（新角色优先，否则沿用当前角色）
            RoleEntity effectiveRole = newRole != null ? newRole
                    : (entity.getRoleId() != null ? roleService.getById(entity.getRoleId()) : null);
            if (effectiveRole != null) {
                // 角色业务规则校验（hospitals范围 + 设计师specialty）
                validateHospitalScope(effectiveRole, dto.getHospitalIds());
                validateSpecialty(effectiveRole, dto.getSpecialty());
            }
            // 更新角色冗余字段，并处理医院范围权限变更
            if (newRole != null) {
                entity.setRoleName(newRole.getRoleName());
                entity.setRoleCode(newRole.getRoleCode());
            }
            if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()
                    && effectiveRole != null
                    && DataScopeTypeEnum.HOSPITALS.getCode().equals(effectiveRole.getDataScopeType())) {
                // 覆盖式分配医院权限（角色变更或编辑页微调均走此路径）
                userHospitalService.assignHospitals(id, dto.getHospitalIds());
            }

            // 更新用户信息（排除不允许通过此接口修改的字段）
            BeanUtils.copyProperties(dto, entity, "id", "username", "password", "status",
                    "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新用户成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (DuplicateKeyException e) {
            log.warn("手机号冲突（并发），id={}", id, e);
            throw new BusinessException(ErrorCodeEnum.USER_PHONE_EXISTS);
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
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 删除用户前先清理医院关联
        userHospitalService.assignHospitals(id, List.of());
        removeById(id);
        log.info("删除用户成功，id={}", id);
    }

    /**
     * 修改用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改用户状态，id={}, status={}", id, status);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        entity.setStatus(status);
        updateById(entity);
        log.info("修改用户状态成功，id={}, status={}", id, status);
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
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, ChangePasswordDTO dto) {
        log.info("修改密码，id={}", id);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 校验旧密码是否正确
        if (!passwordEncoder.matches(dto.getOldPassword(), entity.getPassword())) {
            log.warn("旧密码不正确，id={}", id);
            throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
        }
        // 校验新密码不能与旧密码相同
        if (passwordEncoder.matches(dto.getNewPassword(), entity.getPassword())) {
            log.warn("新密码与旧密码相同，id={}", id);
            throw new BusinessException(ErrorCodeEnum.NEW_PASSWORD_SAME_AS_OLD);
        }
        // 校验新密码强度
        if (!isPasswordStrong(dto.getNewPassword())) {
            log.warn("新密码强度不足，id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_WEAK);
        }
        // 更新密码
        entity.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(entity);
        log.info("修改密码成功，id={}", id);
    }

    /**
     * 用户自更新（仅允许修改手机号和头像）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserBySelf(Long id, com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO dto) {
        log.info("用户自更新信息，id={}", id);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在，id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 校验手机号是否与其他用户重复
        if (dto.getPhone() != null && !dto.getPhone().equals(entity.getPhone())) {
            if (isPhoneExistsExcludingId(dto.getPhone(), id)) {
                log.warn("手机号已存在，phone={}", maskPhone(dto.getPhone()));
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
    }

    // ==================== 私有方法 ====================

    /**
     * 校验角色医院范围权限：当角色 dataScopeType=hospitals 时，hospitalIds 必填且每个ID真实存在
     *
     * @param role        生效角色（null时跳过校验）
     * @param hospitalIds 前端传入的医院ID列表
     */
    private void validateHospitalScope(RoleEntity role, List<Long> hospitalIds) {
        if (role == null || !DataScopeTypeEnum.HOSPITALS.getCode().equals(role.getDataScopeType())) {
            return;
        }
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            log.warn("角色数据权限为医院范围，但未指定医院，roleId={}", role.getId());
            throw new BusinessException(ErrorCodeEnum.USER_ROLE_HOSPITAL_SCOPE_REQUIRED);
        }
        // 批量校验医院ID是否真实存在
        Set<Long> existingIds = hospitalService.listByIds(hospitalIds).stream()
                .map(h -> h.getId())
                .collect(Collectors.toSet());
        List<Long> invalidIds = hospitalIds.stream()
                .filter(hid -> !existingIds.contains(hid))
                .collect(Collectors.toList());
        if (!invalidIds.isEmpty()) {
            log.warn("存在无效的医院ID，invalidHospitalIds={}", invalidIds);
            throw new BusinessException(ErrorCodeEnum.USER_HOSPITAL_INVALID);
        }
    }

    /**
     * 校验设计师专业方向：当角色为 designer/designer-manager 时，specialty 必填且字典编码合法
     *
     * @param role      生效角色（null时跳过校验）
     * @param specialty 专业方向字典编码
     */
    private void validateSpecialty(RoleEntity role, String specialty) {
        if (role == null || !SPECIALTY_REQUIRED_ROLES.contains(role.getRoleCode())) {
            return;
        }
        if (StrUtil.isBlank(specialty)) {
            log.warn("角色为设计师/设计师管理员，但未指定专业方向，roleId={}", role.getId());
            throw new BusinessException(ErrorCodeEnum.USER_ROLE_SPECIALTY_REQUIRED);
        }
        String prefix = DictCodeConstants.USER_SPECIALTY + ".";
        if (!specialty.startsWith(prefix)) {
            log.warn("专业方向字典编码无效，specialty={}", specialty);
            throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, prefix);
        }
        if (dictService.getByDictCode(specialty) == null) {
            log.warn("专业方向字典编码不存在，specialty={}", specialty);
            throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, specialty);
        }
    }

    /**
     * 填充冗余字段（复用已查询的实体，避免重复查询）
     *
     * @param entity 用户实体
     * @param orgEntity 已查询的机构实体（可为空）
     * @param deptEntity 已查询的部门实体（可为空）
     * @param roleEntity 已查询的角色实体（可为空）
     */
    private void fillRedundantFields(UserEntity entity, OrgEntity orgEntity, DeptEntity deptEntity, RoleEntity roleEntity) {
        // 填充机构名称
        if (entity.getOrgId() != null && orgEntity != null) {
            entity.setOrgName(orgEntity.getOrgName());
        }
        // 填充部门名称
        if (entity.getDeptId() != null && deptEntity != null) {
            entity.setDeptName(deptEntity.getDeptName());
        }
        // 填充角色名称和编码
        if (entity.getRoleId() != null && roleEntity != null) {
            entity.setRoleName(roleEntity.getRoleName());
            entity.setRoleCode(roleEntity.getRoleCode());
        }
    }

    /**
     * 校验密码强度
     * 规则：必须包含字母和数字，长度6-20位
     *
     * @param password 密码
     * @return true-强度合格，false-强度不足
     */
    private boolean isPasswordStrong(String password) {
        if (StrUtil.isBlank(password) || password.length() < 6 || password.length() > 20) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }

    /**
     * 脱敏用户名
     * 保留首尾字符，中间用星号替代（复用 Hutool StrUtil.hide）
     *
     * @param username 用户名
     * @return 脱敏后的用户名
     */
    private String maskUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return "***";
        }
        return StrUtil.hide(username, 1, username.length() - 1);
    }

    /**
     * 脱敏手机号
     * 只显示前3后4位
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
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
        // 填充角色的 dataScopeType
        if (vo.getRoleId() != null) {
            RoleEntity roleEntity = roleService.getById(vo.getRoleId());
            if (roleEntity != null) {
                vo.setDataScopeType(roleEntity.getDataScopeType());
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
