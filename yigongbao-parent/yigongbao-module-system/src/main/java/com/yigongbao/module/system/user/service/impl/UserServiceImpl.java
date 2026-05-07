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
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import com.yigongbao.module.system.dept.mapper.DeptOrgMapper;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dict.entity.DictEntity;
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
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final UserHospitalService userHospitalService;
    private final DeptOrgMapper deptOrgMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分页查询用户列表
     * <p>
     * 采用批量查询模式消除 N+1 问题：先分页查出用户实体，再一次性批量拉取
     * 角色、机构、部门、医院关联数据，最后在内存中完成 VO 填充。
     *
     * @param dto 分页查询条件
     * @return 分页用户 VO 列表
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

        // 批量查询关联数据，避免 N+1 问题：若逐条查询，每页10条会产生 10×4 次额外查询
        Map<Long, List<Long>> userHospitalMap = Collections.emptyMap();
        Map<Long, RoleEntity> roleMap = Collections.emptyMap();
        List<UserEntity> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            // 从当前页所有用户中收集去重后的关联 ID，用于后续批量 IN 查询
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

            // 批量查询角色信息，转为 Map 供 O(1) 查找
            roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                    : roleService.listByIds(roleIds).stream()
                            .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
            // 批量查询机构信息，转为 Map 供 O(1) 查找
            Map<Long, OrgEntity> orgMap = orgIds.isEmpty() ? Collections.emptyMap()
                    : orgService.listByIds(orgIds).stream()
                            .collect(Collectors.toMap(OrgEntity::getId, Function.identity()));
            // 批量查询部门信息，转为 Map 供 O(1) 查找
            Map<Long, DeptEntity> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                    : deptService.listByIds(deptIds).stream()
                            .collect(Collectors.toMap(DeptEntity::getId, Function.identity()));

            // 批量查询用户-医院关联（一次 IN 查询替代逐用户查询）
            userHospitalMap = userHospitalService.listHospitalIdsByUserIds(
                    records.stream().map(UserEntity::getId).filter(Objects::nonNull).collect(Collectors.toList()));

            // 将批量查询结果填充到实体冗余字段，避免 VO 转换后再单条查询
            for (UserEntity entity : records) {
                fillEntityWithNames(entity, roleMap, orgMap, deptMap);
            }
        }

        IPage<UserVO> voPage = pageResult.convert(UserConvert::toVO);
        // 收集本页所有用户关联的医院 ID，再批量查一次医院名称（避免逐医院查询）
        Set<Long> allHospitalIds = userHospitalMap.values().stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> hospitalNameMap = allHospitalIds.isEmpty() ? Collections.emptyMap()
                : orgService.listByIds(allHospitalIds).stream()
                        .collect(Collectors.toMap(OrgEntity::getId, OrgEntity::getOrgName));
        // 转换后再填充 VO 中需要名称的字段和医院ID列表
        for (UserVO vo : voPage.getRecords()) {
            if (vo.getStatus() != null) {
                vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
            }
            if (StrUtil.isNotBlank(vo.getSex())) {
                var sexDict = dictService.getByDictCode(vo.getSex());
                vo.setSexName(sexDict != null ? sexDict.getDictName() : "");
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
            // 填充医院ID列表和医院名称列表
            if (vo.getId() != null) {
                List<Long> hospitalIds = userHospitalMap.getOrDefault(vo.getId(), Collections.emptyList());
                vo.setHospitalIds(hospitalIds);
                vo.setHospitalNames(hospitalIds.stream()
                        .map(hospitalNameMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            // 填充专业方向多值列表及名称列表
            if (StrUtil.isNotBlank(vo.getSpecialty())) {
                List<String> specList = StrUtil.split(vo.getSpecialty(), ',');
                vo.setSpecialtyList(specList);
                List<String> nameList = specList.stream()
                        .map(code -> {
                            var dict = dictService.getByDictCode(code);
                            return dict != null ? dict.getDictName() : code;
                        })
                        .collect(Collectors.toList());
                vo.setSpecialtyNameList(nameList);
                vo.setSpecialtyName(String.join(",", nameList));
            }
            // 填充结算类型名称
            if (vo.getSettlementType() != null) {
                // settlementType 存储的是整数值 1/2/3，对应字典 8.1/8.2/8.3
                // 结算类型父节点 dict_code=DictCodeConstants.SETTLEMENT_TYPE("8")，其数据库 id=36
                DictEntity dictEntity = dictService.lambdaQuery()
                        .eq(DictEntity::getParentId, 36L)
                        .eq(DictEntity::getDictValue, vo.getSettlementType().toString())
                        .one();
                vo.setSettlementTypeName(dictEntity != null ? dictEntity.getDictName() : null);
            }
        }
        log.info("分页查询用户列表成功，总数={}", pageResult.getTotal());
        return voPage;
    }

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户 VO（含关联名称、医院列表、专业方向等）
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
     * <p>
     * 由 listUser 调用，传入已批量查好的 Map，避免在循环内单条查询数据库。
     * 冗余字段写入 Entity 而非 VO，是因为 UserConvert.toVO 会一并复制这些字段。
     *
     * @param entity  用户实体
     * @param roleMap 角色 Map（key=roleId）
     * @param orgMap  机构 Map（key=orgId）
     * @param deptMap 部门 Map（key=deptId）
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
     * <p>
     * 核心流程：唯一性校验 → 机构/部门存在性校验 → 部门类型分支处理 →
     * 角色校验 → 业务规则校验（医院范围/专业方向）→ 密码加密 → 持久化 → 医院权限分配。
     * <p>
     * 配置键：{@link SystemConfigKeyEnum#DEFAULT_PASSWORD}（默认密码），
     * {@link SystemConfigKeyEnum#MANUFACTURER_ORG_ID}（生产企业机构ID）。
     *
     * @param dto 创建用户请求 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(CreateUserDTO dto) {
        log.info("创建用户，orgId={}", dto.getOrgId());
        try {
            // 判断是否开启用户名自动生成（在所有校验之前，确定 username 来源）
            boolean autoGenerate = Boolean.parseBoolean(
                configService.getConfigValue(SystemConfigKeyEnum.USER_USERNAME_AUTO_GENERATE.getKey()));
            if (!autoGenerate) {
                // 手动模式：username 必填
                if (StrUtil.isBlank(dto.getUsername())) {
                    throw new BusinessException(ErrorCodeEnum.USER_USERNAME_REQUIRED);
                }
            }
            // 校验用户名全局唯一（函数索引保障 DB 层，此处提前拦截给出友好提示）
            if (StrUtil.isNotBlank(dto.getUsername()) && isUsernameExists(dto.getUsername())) {
                log.warn("用户名已存在，username={}", maskUsername(dto.getUsername()));
                throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
            }
            // 校验手机号全局唯一
            if (isPhoneExists(dto.getPhone())) {
                log.warn("手机号已存在，phone={}", maskPhone(dto.getPhone()));
                throw new BusinessException(ErrorCodeEnum.USER_PHONE_EXISTS);
            }
            // 邮箱为选填字段，非空时才做唯一性校验
            if (StrUtil.isNotBlank(dto.getEmail()) && isEmailExists(dto.getEmail())) {
                log.warn("邮箱已存在，email={}", dto.getEmail());
                throw new BusinessException(ErrorCodeEnum.USER_EMAIL_EXISTS);
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
            // 校验所属部门是否存在（部门类型决定后续机构归属分支，必须先查出实体）
            DeptEntity deptEntity = null;
            if (dto.getDeptId() != null) {
                deptEntity = deptService.getById(dto.getDeptId());
                if (deptEntity == null) {
                    log.warn("所属部门不存在，deptId={}", dto.getDeptId());
                    throw new BusinessException(ErrorCodeEnum.USER_DEPT_NOT_FOUND);
                }
            }
            // 根据部门类型走不同的机构归属校验分支（内部部门强制绑定生产企业，外部部门校验 orgId 属于该部门）
            if (deptEntity != null) {
                Integer deptType = deptEntity.getDeptType();
                if (Integer.valueOf(1).equals(deptType)) {
                    // 内部部门（deptType=1）：强制覆盖 orgId 为生产企业，防止前端伪造归属；同时要求工号非空
                    String manufacturerOrgIdStr = configService.getConfigValue(SystemConfigKeyEnum.MANUFACTURER_ORG_ID.getKey());
                    if (StrUtil.isBlank(manufacturerOrgIdStr)) {
                        log.error("系统配置缺失：{}，无法创建内部用户", SystemConfigKeyEnum.MANUFACTURER_ORG_ID.getKey());
                        throw new BusinessException(ErrorCodeEnum.SYSTEM_CONFIG_MISSING);
                    }
                    Long manufacturerOrgId = Long.valueOf(manufacturerOrgIdStr);
                    // 强制将 orgId 设为生产企业，忽略前端传入值
                    dto.setOrgId(manufacturerOrgId);
                    orgEntity = orgService.getById(manufacturerOrgId);
                    if (StrUtil.isBlank(dto.getEmployeeNo())) {
                        throw new BusinessException(ErrorCodeEnum.EMPLOYEE_NO_REQUIRED);
                    }
                } else if (Integer.valueOf(2).equals(deptType)) {
                    // 外部部门（deptType=2）：orgId 必填，且该机构必须已关联到此部门
                    if (dto.getOrgId() == null) {
                        throw new BusinessException(ErrorCodeEnum.ORG_NOT_BELONG_TO_DEPT);
                    }
                    // 查询该部门下所有关联机构 ID，校验前端传入的 orgId 是否在其中
                    List<Long> deptOrgIds = deptOrgMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeptOrgEntity>()
                                    .eq(DeptOrgEntity::getDeptId, dto.getDeptId()))
                            .stream().map(DeptOrgEntity::getOrgId).collect(java.util.stream.Collectors.toList());
                    if (!deptOrgIds.contains(dto.getOrgId())) {
                        log.warn("机构不属于该部门，deptId={}, orgId={}", dto.getDeptId(), dto.getOrgId());
                        throw new BusinessException(ErrorCodeEnum.ORG_NOT_BELONG_TO_DEPT);
                    }
                    // 外部部门仅允许经销商类型机构，防止将医院直接挂到外部部门
                    OrgEntity extOrg = orgService.getById(dto.getOrgId());
                    if (extOrg == null || !DictCodeConstants.ORG_TYPE_DEALER.equals(extOrg.getOrgType())) {
                        log.warn("机构类型不是经销商，orgId={}", dto.getOrgId());
                        throw new BusinessException(ErrorCodeEnum.ORG_TYPE_MUST_BE_DEALER);
                    }
                    // hospitalIds 已由前端传入，后续统一由 userHospitalService 处理
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
            validateSpecialty(roleEntity, dto.getSpecialtyList());
            // DTO转换为实体对象
            UserEntity entity = UserConvert.toEntity(dto);
            // specialty 在 DB 中以逗号分隔字符串存储，前端传 List 需在此转换
            if (CollUtil.isNotEmpty(dto.getSpecialtyList())) {
                entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
            } else {
                entity.setSpecialty(null);
            }
            // 填充冗余字段（复用已查询的实体，避免重复查询）
            fillRedundantFields(entity, orgEntity, deptEntity, roleEntity);
            // 自动生成模式：在 orgId 最终确定后生成 username
            if (autoGenerate) {
                Long currentUserId = StpUtil.getLoginIdAsLong();
                String redisKey = "username:reserve:" + currentUserId + ":" + entity.getOrgId();
                String reserved = stringRedisTemplate.opsForValue().get(redisKey);
                if (StrUtil.isNotBlank(reserved)) {
                    // 使用预占值，消费后删除
                    entity.setUsername(reserved);
                    stringRedisTemplate.delete(redisKey);
                    log.info("使用预占用户名，username={}", reserved);
                } else {
                    // 无预占（超时或未预览），重新生成
                    entity.setUsername(generateUsername(entity.getOrgId()));
                    log.info("重新生成用户名，username={}", entity.getUsername());
                }
            }
            // 密码加密存储（如果未提供密码则从系统配置获取默认密码）
            String rawPassword;
            if (StrUtil.isNotBlank(dto.getPassword())) {
                rawPassword = dto.getPassword();
                // 密码强度校验：必须包含字母和数字，长度 6-20 位
                if (!isPasswordStrong(rawPassword)) {
                    log.warn("密码强度不足，username={}", maskUsername(entity.getUsername()));
                    throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_WEAK);
                }
            } else {
                // 从系统配置获取默认密码（已内置兜底逻辑，不会返回 null）
                rawPassword = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
                log.info("使用系统配置默认密码，configKey={}", SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
            }
            entity.setPassword(passwordEncoder.encode(rawPassword));
            entity.setStatus(StatusConstants.NORMAL);
            // 空字符串字段统一置 null，避免触发唯一函数索引冲突（NULL 不参与唯一约束）
            if (StrUtil.isBlank(entity.getEmployeeNo())) {
                entity.setEmployeeNo(null);
            }
            if (StrUtil.isBlank(entity.getEmail())) {
                entity.setEmail(null);
            }
            // 插入数据库
            save(entity);

            // 仅当角色 dataScopeType=HOSPITALS 时才写入医院权限关联，其他数据范围类型无需此表
            if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                if (roleEntity != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(roleEntity.getDataScopeType())) {
                    userHospitalService.assignHospitals(entity.getId(), dto.getHospitalIds());
                }
            }

            log.info("创建用户成功，id={}, username={}", entity.getId(), maskUsername(entity.getUsername()));
        } catch (BusinessException e) {
            throw e;
        } catch (DuplicateKeyException e) {
            log.warn("用户名或手机号冲突（并发），username={}", maskUsername(dto.getUsername()), e);
            throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
        } catch (Exception e) {
            log.error("创建用户异常，orgId={}", dto.getOrgId(), e);
            throw e;
        }
    }

    /**
     * 更新用户
     * <p>
     * 角色生效规则：若本次传入了新 roleId 则以新角色为准，否则沿用用户当前角色。
     * 医院权限采用覆盖式更新：只要传入 hospitalIds 且生效角色为 HOSPITALS 范围，即全量替换。
     *
     * @param id  用户ID
     * @param dto 更新用户请求 DTO
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
            // 校验邮箱是否与其他用户重复
            if (StrUtil.isNotBlank(dto.getEmail()) && !dto.getEmail().equals(entity.getEmail())) {
                if (isEmailExistsExcludingId(dto.getEmail(), id)) {
                    log.warn("邮箱已存在，email={}", dto.getEmail());
                    throw new BusinessException(ErrorCodeEnum.USER_EMAIL_EXISTS);
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
            // 确定本次操作生效的角色：新角色优先，否则沿用当前角色（避免因未传 roleId 而跳过业务规则校验）
            RoleEntity effectiveRole = newRole != null ? newRole
                    : (entity.getRoleId() != null ? roleService.getById(entity.getRoleId()) : null);
            if (effectiveRole != null) {
                // 角色业务规则校验（hospitals范围 + 设计师specialty）
                validateHospitalScope(effectiveRole, dto.getHospitalIds());
                validateSpecialty(effectiveRole, dto.getSpecialtyList());
            }
            // 更新角色冗余字段，并处理医院范围权限变更
            if (newRole != null) {
                entity.setRoleName(newRole.getRoleName());
                entity.setRoleCode(newRole.getRoleCode());
            }
            if (effectiveRole != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(effectiveRole.getDataScopeType())) {
                if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                    // 覆盖式分配医院权限：先清空再写入，保证与前端选择完全一致
                    userHospitalService.assignHospitals(id, dto.getHospitalIds());
                }
            } else if (newRole != null && !DataScopeTypeEnum.HOSPITALS.getCode().equals(newRole.getDataScopeType())) {
                // 角色从 HOSPITALS 切换为其他类型时，清理旧的医院权限记录
                userHospitalService.assignHospitals(id, java.util.Collections.emptyList());
            }
            if (dto.getSpecialtyList() != null) {
                entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
            }

            // 更新用户信息（排除不允许通过此接口修改的字段）
            BeanUtils.copyProperties(dto, entity, "id", "username", "password", "status",
                    "createTime", "updateTime", "createBy", "updateBy");
            // 空字符串工号统一置 null，避免触发唯一函数索引冲突（NULL 不参与唯一约束）
            if (StrUtil.isBlank(entity.getEmployeeNo())) {
                entity.setEmployeeNo(null);
            }
            if (StrUtil.isBlank(entity.getEmail())) {
                entity.setEmail(null);
            }
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
        // 清空以该用户为负责人的部门记录，避免悬空引用
        deptService.lambdaUpdate()
                .eq(DeptEntity::getLeaderUserId, id)
                .set(DeptEntity::getLeaderUserId, null)
                .update();
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
        // 禁用用户时，强制踢出其当前会话，使 token 立即失效
        if (StatusConstants.DISABLED == status) {
            try {
                cn.dev33.satoken.stp.StpUtil.kickout(id);
                log.info("已强制踢出用户会话，userId={}", id);
            } catch (Exception ex) {
                log.warn("踢出用户会话失败（用户可能未登录），userId={}", id);
            }
        }
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
     * 校验角色医院范围权限
     * <p>
     * 当角色 dataScopeType=HOSPITALS 时，hospitalIds 必填且每个 ID 必须是真实存在的医院机构
     * （orgType=ORG_TYPE_HOSPITAL）。其他数据范围类型直接跳过，无需传医院列表。
     *
     * @param role        生效角色（null 时跳过校验）
     * @param hospitalIds 前端传入的医院 ID 列表
     */
    private void validateHospitalScope(RoleEntity role, List<Long> hospitalIds) {
        // 非医院范围角色无需校验，直接返回
        if (role == null || !DataScopeTypeEnum.HOSPITALS.getCode().equals(role.getDataScopeType())) {
            return;
        }
        // 医院范围角色必须指定至少一家医院
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            log.warn("角色数据权限为医院范围，但未指定医院，roleId={}", role.getId());
            throw new BusinessException(ErrorCodeEnum.USER_ROLE_HOSPITAL_SCOPE_REQUIRED);
        }
        // 批量查询并过滤出真实医院（orgType=ORG_TYPE_HOSPITAL），与传入列表取差集得到无效 ID
        Set<Long> existingIds = orgService.listByIds(hospitalIds).stream()
                .filter(org -> DictCodeConstants.ORG_TYPE_HOSPITAL.equals(org.getOrgType()))
                .map(OrgEntity::getId)
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
     * 校验设计师专业方向
     * <p>
     * 仅对 designer / designer-manager 角色生效：至少选择一个方向，且每个字典编码必须以
     * {@link com.yigongbao.common.constant.DictCodeConstants#USER_SPECIALTY} 为前缀并在字典表中存在。
     * 其他角色直接跳过，无需传专业方向。
     *
     * @param role          生效角色（null 时跳过校验）
     * @param specialtyList 专业方向字典编码列表
     */
    private void validateSpecialty(RoleEntity role, List<String> specialtyList) {
        // 非设计师角色无需专业方向，直接跳过
        if (role == null || role.getRoleCode() == null
                || !SPECIALTY_REQUIRED_ROLES.contains(role.getRoleCode())) {
            return;
        }
        // 设计师/设计师管理员必须至少选择一个专业方向
        if (CollUtil.isEmpty(specialtyList)) {
            log.warn("角色为设计师/设计师管理员，但未指定专业方向，roleId={}", role.getId());
            throw new BusinessException(ErrorCodeEnum.USER_ROLE_SPECIALTY_REQUIRED);
        }
        String prefix = DictCodeConstants.USER_SPECIALTY + ".";
        for (String specialty : specialtyList) {
            // 校验编码格式：必须以 USER_SPECIALTY 前缀开头，防止传入非专业方向字典值
            if (StrUtil.isBlank(specialty) || !specialty.startsWith(prefix)) {
                log.warn("专业方向字典编码无效，specialty={}", specialty);
                throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, prefix);
            }
            // 校验编码在字典表中真实存在，防止传入已废弃或伪造的编码
            if (dictService.getByDictCode(specialty) == null) {
                log.warn("专业方向字典编码不存在，specialty={}", specialty);
                throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, specialty);
            }
        }
    }

    /**
     * 填充用户实体冗余字段
     * <p>
     * 冗余字段（orgName/deptName/roleName/roleCode）存储在用户表中，目的是避免列表查询时
     * 每行都 JOIN 三张关联表，以空间换时间。复用调用方已查询的实体对象，不再重复查库。
     *
     * @param entity     用户实体
     * @param orgEntity  已查询的机构实体（可为 null）
     * @param deptEntity 已查询的部门实体（可为 null）
     * @param roleEntity 已查询的角色实体（可为 null）
     */
    private void fillRedundantFields(UserEntity entity, OrgEntity orgEntity, DeptEntity deptEntity, RoleEntity roleEntity) {
        // 冗余机构名称，避免查询时 JOIN sys_org
        if (entity.getOrgId() != null && orgEntity != null) {
            entity.setOrgName(orgEntity.getOrgName());
        }
        // 冗余部门名称，避免查询时 JOIN sys_dept
        if (entity.getDeptId() != null && deptEntity != null) {
            entity.setDeptName(deptEntity.getDeptName());
        }
        // 冗余角色名称和编码，roleCode 用于前端权限判断，避免查询时 JOIN sys_role
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
        if (StrUtil.isNotBlank(vo.getSex())) {
            var sexDict = dictService.getByDictCode(vo.getSex());
            vo.setSexName(sexDict != null ? sexDict.getDictName() : "");
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
        // 填充医院ID列表和医院名称列表
        List<Long> hospitalIds = userHospitalService.getHospitalIdsByUserId(vo.getId());
        vo.setHospitalIds(hospitalIds);
        if (!hospitalIds.isEmpty()) {
            List<String> hospitalNames = orgService.listByIds(hospitalIds).stream()
                    .map(OrgEntity::getOrgName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            vo.setHospitalNames(hospitalNames);
        } else {
            vo.setHospitalNames(Collections.emptyList());
        }
        // 填充专业方向多值列表及名称列表
        if (StrUtil.isNotBlank(vo.getSpecialty())) {
            List<String> specList = StrUtil.split(vo.getSpecialty(), ',');
            vo.setSpecialtyList(specList);
            List<String> nameList = specList.stream()
                    .map(code -> {
                        var dict = dictService.getByDictCode(code);
                        return dict != null ? dict.getDictName() : code;
                    })
                    .collect(Collectors.toList());
            vo.setSpecialtyNameList(nameList);
            // 保持 specialtyName 向后兼容（逗号拼接）
            vo.setSpecialtyName(String.join(",", nameList));
        }
        // 填充结算类型名称
        if (vo.getSettlementType() != null) {
            // 结算类型父节点 dict_code=DictCodeConstants.SETTLEMENT_TYPE("8")，其数据库 id=36
            DictEntity dictEntity = dictService.lambdaQuery()
                    .eq(DictEntity::getParentId, 36L)
                    .eq(DictEntity::getDictValue, vo.getSettlementType().toString())
                    .one();
            vo.setSettlementTypeName(dictEntity != null ? dictEntity.getDictName() : null);
        }
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

    /**
     * 校验邮箱是否存在
     */
    private boolean isEmailExists(String email) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, email)) > 0;
    }

    /**
     * 校验邮箱是否存在（排除指定ID）
     */
    private boolean isEmailExistsExcludingId(String email, Long excludeId) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, email)
                .ne(UserEntity::getId, excludeId)) > 0;
    }

    /**
     * 统计指定部门下的用户数量
     *
     * @param deptId 部门ID
     * @return 用户数量
     */
    @Override
    public long countByDeptId(Long deptId) {
        return baseMapper.countByDeptId(deptId);
    }

    /**
     * 查询指定部门下所有启用状态的用户ID列表
     *
     * @param deptId 部门ID
     * @return 用户ID列表
     */
    @Override
    public List<Long> listUserIdsByDeptId(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getDeptId, deptId)
                .eq(UserEntity::getStatus, StatusConstants.NORMAL)
                .eq(UserEntity::getIsDeleted, StatusConstants.NOT_DELETED))
                .stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
    }

    /**
     * 根据机构ID查询用户ID列表
     *
     * @param orgId 机构ID
     * @return 用户ID列表
     */
    @Override
    public List<Long> listUserIdsByOrgId(Long orgId) {
        if (orgId == null) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getOrgId, orgId)
                .eq(UserEntity::getStatus, StatusConstants.NORMAL)
                .eq(UserEntity::getIsDeleted, StatusConstants.NOT_DELETED))
                .stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
    }

    /**
     * 预览用户名（自动生成模式，预占5分钟）
     *
     * @param orgId 机构ID
     * @return 预占的用户名，手动模式返回 null
     */
    @Override
    public String previewUsername(Long orgId) {
        boolean autoGenerate = Boolean.parseBoolean(
            configService.getConfigValue(SystemConfigKeyEnum.USER_USERNAME_AUTO_GENERATE.getKey()));
        if (!autoGenerate) return null;
        // 生成真实序号并预占
        String username = generateUsername(orgId);
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String redisKey = "username:reserve:" + currentUserId + ":" + orgId;
        stringRedisTemplate.opsForValue().set(redisKey, username, 5, TimeUnit.MINUTES);
        log.info("预占用户名，orgId={}, username={}", orgId, username);
        return username;
    }

    /**
     * 按机构前缀生成用户名，格式为 {prefix}{seq3位补零}（如 ceshi001）
     *
     * @param orgId 机构ID
     * @return 生成的用户名
     */
    private String generateUsername(Long orgId) {
        OrgEntity org = orgService.getById(orgId);
        if (org == null || StrUtil.isBlank(org.getUsernamePrefix())) {
            throw new BusinessException(ErrorCodeEnum.ORG_USERNAME_PREFIX_MISSING);
        }
        // generateWithSeqSuffix 返回格式为 "prefix-N"，解析后格式化为 "prefixNNN"
        String raw = codeGeneratorService.generateWithSeqSuffix(CodeRuleConstants.USER_NO, org.getUsernamePrefix());
        int dashIdx = raw.lastIndexOf('-');
        String prefix = raw.substring(0, dashIdx);
        long seq = Long.parseLong(raw.substring(dashIdx + 1));
        return prefix + String.format("%03d", seq);
    }
}
