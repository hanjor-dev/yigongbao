package com.yigongbao.module.system.user.service.impl;

/**
 * 用户管理 Service 实现类
 * 处理用户相关的业务逻辑: 包括用户CRUD、密码管理、状态管理等
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
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import com.yigongbao.module.system.dept.mapper.DeptOrgMapper;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
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
import com.yigongbao.module.system.user.service.UserManagedOrgService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.vo.ManagedOrgScopeVO;
import com.yigongbao.module.system.user.vo.UserVO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
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
 * 处理用户相关的业务逻辑: 包括用户CRUD、密码管理等
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    /** 需要填写专业方向的角色编码 */
    private static final List<String> SPECIALTY_REQUIRED_ROLES = List.of(RoleCodeEnum.DESIGNER.getCode(), RoleCodeEnum.DESIGNER_MANAGER.getCode());

    private final OrgService orgService;
    private final DeptService deptService;
    private final RoleService roleService;
    private final DictService dictService;
    private final PasswordEncoder passwordEncoder;
    private final ConfigService configService;
    private final UserHospitalService userHospitalService;
    private final UserManagedOrgService userManagedOrgService;
    private final DeptOrgMapper deptOrgMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final StringRedisTemplate stringRedisTemplate;
    private final com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper processingCenterMapper;
    private final com.yigongbao.module.basic.chargingTemplate.service.ChargingTemplateService chargingTemplateService;

    /**
     * 分页查询用户列表
     * <p>
     * 采用批量查询模式消除 N+1 问题：先分页查出用户实体: 再一次性批量拉取
     * 角色、机构、部门、医院关联数据: 最后在内存中完成 VO 填充:
     *
     * @param dto 分页查询条件
     * @return 分页用户 VO 列表
     */
    @Override
    public IPage<UserVO> listUser(UserPageDTO dto) {
        
        // 如果未传入分页参数: 使用默认值
        int pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
        Page<UserEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StrUtil.isNotBlank(dto.getKeyword()), query -> query
                        .like(UserEntity::getUsername, dto.getKeyword())
                        .or()
                        .like(UserEntity::getRealName, dto.getKeyword()))
                .eq(Objects.nonNull(dto.getOrgId()), UserEntity::getOrgId, dto.getOrgId())
                .eq(Objects.nonNull(dto.getDeptId()), UserEntity::getDeptId, dto.getDeptId())
                .eq(StrUtil.isNotBlank(dto.getAccountType()), UserEntity::getAccountType, dto.getAccountType())
                .eq(Objects.nonNull(dto.getStatus()), UserEntity::getStatus, dto.getStatus())
                .orderByDesc(UserEntity::getCreateTime);
        IPage<UserEntity> pageResult = page(page, wrapper);

        // 批量查询关联数据: 避免 N+1 问题：若逐条查询: 每页10条会产生 10×4 次额外查询
        Map<Long, List<Long>> userHospitalMap = Collections.emptyMap();
        Map<Long, RoleEntity> roleMap = Collections.emptyMap();
        List<UserEntity> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            // 从当前页所有用户中收集去重后的关联 ID: 用于后续批量 IN 查询
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

            // 批量查询角色信息: 转为 Map 供 O(1) 查找
            roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                    : roleService.listByIds(roleIds).stream()
                            .collect(Collectors.toMap(RoleEntity::getId, Function.identity()));
            // 批量查询机构信息: 转为 Map 供 O(1) 查找
            Map<Long, OrgEntity> orgMap = orgIds.isEmpty() ? Collections.emptyMap()
                    : orgService.listByIds(orgIds).stream()
                            .collect(Collectors.toMap(OrgEntity::getId, Function.identity()));
            // 批量查询部门信息: 转为 Map 供 O(1) 查找
            Map<Long, DeptEntity> deptMap = deptIds.isEmpty() ? Collections.emptyMap()
                    : deptService.listByIds(deptIds).stream()
                            .collect(Collectors.toMap(DeptEntity::getId, Function.identity()));

            // 批量查询收费模板信息: 转为 Map 供 O(1) 查找
            Set<Long> templateIds = records.stream()
                    .map(UserEntity::getChargingTemplateId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity> templateMap =
                    templateIds.isEmpty() ? Collections.emptyMap()
                    : chargingTemplateService.listByIds(templateIds).stream()
                            .collect(Collectors.toMap(com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity::getId, Function.identity()));

            // 批量查询用户-医院关联（一次 IN 查询替代逐用户查询）
            userHospitalMap = userHospitalService.listHospitalIdsByUserIds(
                    records.stream().map(UserEntity::getId).filter(Objects::nonNull).collect(Collectors.toList()));

            // 将批量查询结果填充到实体冗余字段: 避免 VO 转换后再单条查询
            for (UserEntity entity : records) {
                fillEntityWithNames(entity, roleMap, orgMap, deptMap, templateMap);
            }
        }

        IPage<UserVO> voPage = pageResult.convert(UserConvert::toVO);
        // 收集本页所有用户关联的医院 ID: 再批量查一次医院名称（避免逐医院查询）
        Set<Long> allHospitalIds = userHospitalMap.values().stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> hospitalNameMap = allHospitalIds.isEmpty() ? Collections.emptyMap()
                : orgService.listByIds(allHospitalIds).stream()
                        .collect(Collectors.toMap(OrgEntity::getId, OrgEntity::getOrgName));

        // 批量查询字典: 避免循环中逐条查询（N+1 问题）
        Map<String, String> dictNameMap = Collections.emptyMap();
        Map<Integer, String> settlementTypeNameMap = Collections.emptyMap();
        if (!voPage.getRecords().isEmpty()) {
            // 收集所有需要的字典编码（去重）
            Set<String> dictCodes = new java.util.HashSet<>();
            Set<Integer> settlementTypes = new java.util.HashSet<>();
            for (UserVO vo : voPage.getRecords()) {
                if (StrUtil.isNotBlank(vo.getSex())) dictCodes.add(vo.getSex());
                if (vo.getAccountType() != null) dictCodes.add(vo.getAccountType());
                if (StrUtil.isNotBlank(vo.getSpecialty())) {
                    dictCodes.addAll(StrUtil.split(vo.getSpecialty(), ','));
                }
                if (vo.getSettlementType() != null) settlementTypes.add(vo.getSettlementType());
            }
            // 批量查询字典并构建 Map（dictCode -> dictName）
            if (!dictCodes.isEmpty()) {
                dictNameMap = dictCodes.stream()
                        .map(dictService::getByDictCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(DictVO::getDictCode, DictVO::getDictName));
            }
            // 批量查询结算类型字典（parentId=36: 通过 dictValue 查询）
            if (!settlementTypes.isEmpty()) {
                settlementTypeNameMap = dictService.lambdaQuery()
                        .eq(DictEntity::getParentId, 36L)
                        .in(DictEntity::getDictValue, settlementTypes.stream()
                                .map(Object::toString).collect(Collectors.toList()))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(
                                d -> Integer.parseInt(d.getDictValue()),
                                DictEntity::getDictName));
            }
        }

        // 转换后再填充 VO 中需要名称的字段和医院ID列表
        Map<String, String> finalDictMap = dictNameMap;
        Map<Integer, String> finalSettlementTypeMap = settlementTypeNameMap;
        for (UserVO vo : voPage.getRecords()) {
            if (vo.getStatus() != null) {
                vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
            }
            if (StrUtil.isNotBlank(vo.getSex())) {
                vo.setSexName(finalDictMap.getOrDefault(vo.getSex(), ""));
            }
            if (vo.getAccountType() != null) {
                vo.setAccountTypeName(finalDictMap.getOrDefault(vo.getAccountType(), ""));
            }
            // 填充角色的 dataScopeType
            if (vo.getRoleId() != null) {
                RoleEntity roleEntity = roleMap.get(vo.getRoleId());
                if (roleEntity != null) {
                    vo.setDataScopeType(roleEntity.getDataScopeType());
                    if (RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(roleEntity.getRoleCode())) {
                        ManagedOrgScopeVO scope = userManagedOrgService.getManagedOrgScope(vo.getId(), vo.getOrgId());
                        vo.setManagedOrgIds(scope.getManagedOrgIds());
                        vo.setEffectiveOrgIds(scope.getEffectiveOrgIds());
                    }
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
                        .map(code -> finalDictMap.getOrDefault(code, code))
                        .collect(Collectors.toList());
                vo.setSpecialtyNameList(nameList);
                vo.setSpecialtyName(String.join(",", nameList));
            }
            // 填充结算类型名称
            if (vo.getSettlementType() != null) {
                vo.setSettlementTypeName(finalSettlementTypeMap.get(vo.getSettlementType()));
            }
        }
        
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
        
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        UserVO vo = toVOWithNames(entity);
        vo.setManagedOrgs(Collections.emptyList());
        if (vo.getRoleId() != null) {
            RoleEntity roleEntity = roleService.getById(vo.getRoleId());
            if (roleEntity != null) {
                vo.setDataScopeType(roleEntity.getDataScopeType());
                if (RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(roleEntity.getRoleCode())) {
                    ManagedOrgScopeVO scope = userManagedOrgService.getManagedOrgScope(vo.getId(), vo.getOrgId());
                    vo.setManagedOrgIds(scope.getManagedOrgIds());
                    vo.setManagedOrgs(scope.getManagedOrgs());
                    vo.setEffectiveOrgIds(scope.getEffectiveOrgIds());
                }
            }
        }
        return vo;
    }

    /**
     * 填充实体关联名称（使用批量查询的 Map 数据）
     * <p>
     * 由 listUser 调用: 传入已批量查好的 Map: 避免在循环内单条查询数据库:
     * 冗余字段写入 Entity 而非 VO: 是因为 UserConvert.toVO 会一并复制这些字段:
     *
     * @param entity  用户实体
     * @param roleMap 角色 Map（key=roleId）
     * @param orgMap  机构 Map（key=orgId）
     * @param deptMap 部门 Map（key=deptId）
     * @param templateMap 收费模板 Map（key=templateId）
     */
    private void fillEntityWithNames(UserEntity entity, Map<Long, RoleEntity> roleMap,
                                    Map<Long, OrgEntity> orgMap, Map<Long, DeptEntity> deptMap,
                                    Map<Long, com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity> templateMap) {
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
        // 填充收费模板名称
        if (entity.getChargingTemplateId() != null) {
            com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity template = templateMap.get(entity.getChargingTemplateId());
            if (template != null) {
                entity.setChargingTemplateName(template.getTemplateName());
            }
        }
    }

    /**
     * 创建用户
     * <p>
     * 核心流程：唯一性校验 → 机构/部门存在性校验 → 部门类型分支处理 →
     * 角色校验 → 业务规则校验（医院范围/专业方向）→ 密码加密 → 持久化 → 医院权限分配:
     * <p>
     * 配置键：{@link SystemConfigKeyEnum#DEFAULT_PASSWORD}（默认密码）: 
     * {@link SystemConfigKeyEnum#MANUFACTURER_ORG_ID}（生产企业机构ID）:
     *
     * @param dto 创建用户请求 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(CreateUserDTO dto) {
        log.info("创建用户: orgId={}", dto.getOrgId());
        try {
            // 判断是否开启用户名自动生成（在所有校验之前: 确定 username 来源）
            boolean autoGenerate = Boolean.parseBoolean(
                configService.getConfigValue(SystemConfigKeyEnum.USER_USERNAME_AUTO_GENERATE.getKey()));
            if (!autoGenerate) {
                // 手动模式：username 必填
                if (StrUtil.isBlank(dto.getUsername())) {
                    throw new BusinessException(ErrorCodeEnum.USER_USERNAME_REQUIRED);
                }
            }
            // 校验用户名全局唯一（函数索引保障 DB 层: 此处提前拦截给出友好提示）
            if (StrUtil.isNotBlank(dto.getUsername()) && isUsernameExists(dto.getUsername())) {
                log.warn("用户名已存在: username={}", maskUsername(dto.getUsername()));
                throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
            }
            // 邮箱为选填字段: 非空时才做唯一性校验
            if (StrUtil.isNotBlank(dto.getEmail()) && isEmailExists(dto.getEmail())) {
                log.warn("邮箱已存在: email={}", dto.getEmail());
                throw new BusinessException(ErrorCodeEnum.USER_EMAIL_EXISTS);
            }
            // 校验真实姓名在同角色下的唯一性
            if (isRealNameExistsForRole(dto.getRealName(), dto.getRoleId())) {
                log.warn("该角色下已存在同名用户: realName={}, roleId={}", dto.getRealName(), dto.getRoleId());
                throw new BusinessException(ErrorCodeEnum.USER_REALNAME_EXISTS_IN_ROLE);
            }
            // 先加载角色，机构类型校验需要识别区域管理员
            RoleEntity roleEntity = null;
            if (dto.getRoleId() != null) {
                roleEntity = roleService.getById(dto.getRoleId());
                if (roleEntity == null) {
                    log.warn("角色不存在: roleId={}", dto.getRoleId());
                    throw new BusinessException(ErrorCodeEnum.USER_ROLE_NOT_FOUND);
                }
            }
            // 校验所属机构是否存在: 并获取名称设置到冗余字段
            OrgEntity orgEntity = null;
            if (dto.getOrgId() != null) {
                orgEntity = orgService.getById(dto.getOrgId());
                if (orgEntity == null) {
                    log.warn("所属机构不存在: orgId={}", dto.getOrgId());
                    throw new BusinessException(ErrorCodeEnum.USER_ORG_NOT_FOUND);
                }
            }
            // 校验所属部门是否存在（部门类型决定后续机构归属分支: 必须先查出实体）
            DeptEntity deptEntity = null;
            if (dto.getDeptId() != null) {
                deptEntity = deptService.getById(dto.getDeptId());
                if (deptEntity == null) {
                    log.warn("所属部门不存在: deptId={}", dto.getDeptId());
                    throw new BusinessException(ErrorCodeEnum.USER_DEPT_NOT_FOUND);
                }
            }
            // 根据部门类型走不同的机构归属校验分支（企业部门强制绑定生产企业: 业务部门校验 orgId 属于该部门）
            if (deptEntity != null) {
                String deptType = deptEntity.getDeptType();
                if (StatusConstants.DEPT_TYPE_ENTERPRISE.equals(deptType)) {
                    // 企业部门（deptType=6.1）：从部门关联动态获取企业机构
                    List<Long> deptOrgIds = deptOrgMapper.selectList(
                        new LambdaQueryWrapper<DeptOrgEntity>()
                            .eq(DeptOrgEntity::getDeptId, dto.getDeptId()))
                        .stream()
                        .map(DeptOrgEntity::getOrgId)
                        .collect(Collectors.toList());

                    // 校验：企业部门必须关联至少1个机构
                    if (deptOrgIds.isEmpty()) {
                        log.warn("企业部门未关联企业机构: deptId={}, deptName={}",
                            dto.getDeptId(), deptEntity.getDeptName());
                        throw new BusinessException(ErrorCodeEnum.DEPT_ENTERPRISE_ORG_NOT_BOUND);
                    }

                    Long enterpriseOrgId;
                    // 如果部门关联多个机构（服务商场景），需前端传入 orgId 指定
                    if (deptOrgIds.size() > 1) {
                        if (dto.getOrgId() == null || !deptOrgIds.contains(dto.getOrgId())) {
                            log.warn("企业部门关联多个机构，需指定具体机构: deptId={}, orgCount={}, providedOrgId={}",
                                dto.getDeptId(), deptOrgIds.size(), dto.getOrgId());
                            throw new BusinessException(ErrorCodeEnum.ORG_NOT_BELONG_TO_DEPT);
                        }
                        enterpriseOrgId = dto.getOrgId();
                    } else {
                        // 部门只关联1个机构（生产企业场景），自动使用
                        enterpriseOrgId = deptOrgIds.get(0);
                    }
                    orgEntity = orgService.getById(enterpriseOrgId);

                    // 校验机构类型：必须是生产企业（1.1）或服务商（1.4）
                    if (!DictCodeConstants.ORG_TYPE_PRODUCER.equals(orgEntity.getOrgType())
                        && !DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(orgEntity.getOrgType())) {
                        log.warn("企业部门关联的机构类型错误: deptId={}, orgId={}, orgType={}",
                            dto.getDeptId(), enterpriseOrgId, orgEntity.getOrgType());
                        throw new BusinessException(ErrorCodeEnum.DEPT_ENTERPRISE_ORG_TYPE_ERROR);
                    }

                    // 强制使用部门关联的企业机构（忽略前端传入的 orgId）
                    dto.setOrgId(enterpriseOrgId);

                    // 企业账户必须填写工号
                    if (StrUtil.isBlank(dto.getEmployeeNo())) {
                        throw new BusinessException(ErrorCodeEnum.EMPLOYEE_NO_REQUIRED);
                    }

                    log.info("企业部门用户机构绑定: username={}, deptId={}, orgId={}, orgType={}, employeeNo={}",
                        dto.getUsername(), dto.getDeptId(), enterpriseOrgId,
                        orgEntity.getOrgType(), dto.getEmployeeNo());
                } else if (StatusConstants.DEPT_TYPE_BUSINESS.equals(deptType)) {
                    // 业务部门（deptType=6.2）：orgId 必填: 且该机构必须已关联到此部门
                    if (dto.getOrgId() == null) {
                        throw new BusinessException(ErrorCodeEnum.ORG_NOT_BELONG_TO_DEPT);
                    }
                    // 查询该部门下所有关联机构 ID: 校验前端传入的 orgId 是否在其中
                    List<Long> deptOrgIds = deptOrgMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeptOrgEntity>()
                                    .eq(DeptOrgEntity::getDeptId, dto.getDeptId()))
                            .stream().map(DeptOrgEntity::getOrgId).collect(java.util.stream.Collectors.toList());
                    if (!deptOrgIds.contains(dto.getOrgId())) {
                        log.warn("机构不属于该部门: deptId={}, orgId={}", dto.getDeptId(), dto.getOrgId());
                        throw new BusinessException(ErrorCodeEnum.ORG_NOT_BELONG_TO_DEPT);
                    }
                    // 普通业务角色仍仅允许经销商；区域管理员允许经销商或服务商
                    OrgEntity extOrg = orgService.getById(dto.getOrgId());
                    boolean regionalManager = roleEntity != null
                            && RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(roleEntity.getRoleCode());
                    boolean allowedType = extOrg != null && (DictCodeConstants.ORG_TYPE_DEALER.equals(extOrg.getOrgType())
                            || (regionalManager && DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(extOrg.getOrgType())));
                    if (!allowedType) {
                        log.warn("业务账户主机构类型不合法: orgId={}, regionalManager={}", dto.getOrgId(), regionalManager);
                        throw new BusinessException(ErrorCodeEnum.ORG_TYPE_MUST_BE_DEALER);
                    }
                    // hospitalIds 已由前端传入: 后续统一由 userHospitalService 处理
                }
            }
            // accountType 为空时默认设置为企业账户（6.1）
            if (dto.getAccountType() == null) {
                dto.setAccountType(StatusConstants.ACCOUNT_TYPE_ENTERPRISE);
                log.info("账户分类未指定: 默认设置为企业账户");
            }
            // 校验部门必填规则和部门类型匹配：dataScopeType为dept或self的角色必须选择部门: 且角色accountType必须与部门deptType匹配
            validateDeptRequired(roleEntity, deptEntity);
            // 角色业务规则校验（hospitals范围 + 设计师specialty）
            validateHospitalScope(roleEntity, dto.getHospitalIds());
            validateManagedOrgScope(roleEntity, dto.getOrgId(), dto.getManagedOrgIds());
            validateSpecialty(roleEntity, dto.getSpecialtyList());
            // 生产员角色校验加工中心
            validateProcessingCenter(roleEntity, dto.getCenterId());
            // DTO转换为实体对象
            UserEntity entity = UserConvert.toEntity(dto);
            // specialty 在 DB 中以逗号分隔字符串存储: 前端传 List 需在此转换
            if (CollUtil.isNotEmpty(dto.getSpecialtyList())) {
                entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
            } else {
                entity.setSpecialty(null);
            }
            // 填充冗余字段（复用已查询的实体: 避免重复查询）
            fillRedundantFields(entity, orgEntity, deptEntity, roleEntity);
            // 自动生成模式：在 orgId 最终确定后生成 username
            if (autoGenerate) {
                Long currentUserId = StpUtil.getLoginIdAsLong();
                String redisKey = "username:reserve:" + currentUserId + ":" + entity.getOrgId();
                String reserved = stringRedisTemplate.opsForValue().get(redisKey);
                if (StrUtil.isNotBlank(reserved)) {
                    // 使用预占值: 消费后删除
                    entity.setUsername(reserved);
                    stringRedisTemplate.delete(redisKey);
                    log.info("使用预占用户名: username={}", reserved);
                } else {
                    // 无预占（超时或未预览）: 重新生成
                    entity.setUsername(generateUsername(entity.getOrgId()));
                    log.info("重新生成用户名: username={}", entity.getUsername());
                }
            }
            // 密码加密存储（如果未提供密码则从系统配置获取默认密码）
            String rawPassword;
            if (StrUtil.isNotBlank(dto.getPassword())) {
                rawPassword = dto.getPassword();
                // 密码强度校验：必须包含字母和数字: 长度 6-20 位
                if (!isPasswordStrong(rawPassword)) {
                    log.warn("密码强度不足: username={}", maskUsername(entity.getUsername()));
                    throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_WEAK);
                }
            } else {
                // 从系统配置获取默认密码（已内置兜底逻辑: 不会返回 null）
                rawPassword = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
                log.info("使用系统配置默认密码: configKey={}", SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
            }
            entity.setPassword(passwordEncoder.encode(rawPassword));
            entity.setStatus(StatusConstants.NORMAL);
            // 空字符串字段统一置 null: 避免触发唯一函数索引冲突（NULL 不参与唯一约束）
            if (StrUtil.isBlank(entity.getEmployeeNo())) {
                entity.setEmployeeNo(null);
            }
            if (StrUtil.isBlank(entity.getEmail())) {
                entity.setEmail(null);
            }
            // 插入数据库
            save(entity);

            if (roleEntity != null && RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(roleEntity.getRoleCode())) {
                userManagedOrgService.replaceManagedOrgIds(entity.getId(), entity.getOrgId(), dto.getManagedOrgIds());
            }

            // 仅当角色 dataScopeType=HOSPITALS 时才写入医院权限关联: 其他数据范围类型无需此表
            if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                if (roleEntity != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(roleEntity.getDataScopeType())) {
                    userHospitalService.assignHospitals(entity.getId(), dto.getHospitalIds());
                }
            }

            log.info("创建用户成功: id={}, username={}", entity.getId(), maskUsername(entity.getUsername()));
        } catch (DuplicateKeyException e) {
            log.warn("用户名冲突（并发）: username={}", maskUsername(dto.getUsername()), e);
            throw new BusinessException(ErrorCodeEnum.USER_EXISTS);
        } catch (Exception e) {
            log.error("创建用户异常: orgId={}", dto.getOrgId(), e);
            throw e;
        }
    }

    /**
     * 更新用户
     * <p>
     * 角色生效规则：若本次传入了新 roleId 则以新角色为准: 否则沿用用户当前角色:
     * 医院权限采用覆盖式更新：只要传入 hospitalIds 且生效角色为 HOSPITALS 范围: 即全量替换:
     *
     * @param id  用户ID
     * @param dto 更新用户请求 DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UpdateUserDTO dto) {
        log.info("更新用户: id={}", id);
        try {
            // 校验用户是否存在
            UserEntity entity = getById(id);
            if (entity == null) {
                log.warn("用户不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            Long originalOrgId = entity.getOrgId();
            // 校验邮箱是否与其他用户重复
            if (StrUtil.isNotBlank(dto.getEmail()) && !dto.getEmail().equals(entity.getEmail())) {
                if (isEmailExistsExcludingId(dto.getEmail(), id)) {
                    log.warn("邮箱已存在: email={}", dto.getEmail());
                    throw new BusinessException(ErrorCodeEnum.USER_EMAIL_EXISTS);
                }
            }
            // 校验真实姓名在同角色下的唯一性（当姓名或角色发生变化时）
            String effectiveRealName = StrUtil.isNotBlank(dto.getRealName()) ? dto.getRealName() : entity.getRealName();
            Long effectiveRoleId = dto.getRoleId() != null ? dto.getRoleId() : entity.getRoleId();
            if ((StrUtil.isNotBlank(dto.getRealName()) && !dto.getRealName().equals(entity.getRealName()))
                    || (dto.getRoleId() != null && !dto.getRoleId().equals(entity.getRoleId()))) {
                if (isRealNameExistsForRoleExcludingId(effectiveRealName, effectiveRoleId, id)) {
                    log.warn("该角色下已存在同名用户: realName={}, roleId={}", effectiveRealName, effectiveRoleId);
                    throw new BusinessException(ErrorCodeEnum.USER_REALNAME_EXISTS_IN_ROLE);
                }
            }
            // 校验所属机构是否存在
            if (dto.getOrgId() != null && !dto.getOrgId().equals(entity.getOrgId())) {
                OrgEntity orgEntity = orgService.getById(dto.getOrgId());
                if (orgEntity == null) {
                    log.warn("所属机构不存在: orgId={}", dto.getOrgId());
                    throw new BusinessException(ErrorCodeEnum.USER_ORG_NOT_FOUND);
                }
                // 更新机构名称冗余字段
                entity.setOrgName(orgEntity.getOrgName());
            }
            // 校验所属部门是否存在
            if (dto.getDeptId() != null && !dto.getDeptId().equals(entity.getDeptId())) {
                DeptEntity deptEntity = deptService.getById(dto.getDeptId());
                if (deptEntity == null) {
                    log.warn("所属部门不存在: deptId={}", dto.getDeptId());
                    throw new BusinessException(ErrorCodeEnum.USER_DEPT_NOT_FOUND);
                }
                // 更新部门名称冗余字段
                entity.setDeptName(deptEntity.getDeptName());
            }
            // 角色校验：先查存在性: 再做业务规则校验: 复用同一次查询结果
            RoleEntity newRole = null;
            if (dto.getRoleId() != null && !dto.getRoleId().equals(entity.getRoleId())) {
                newRole = roleService.getById(dto.getRoleId());
                if (newRole == null) {
                    log.warn("角色不存在: roleId={}", dto.getRoleId());
                    throw new BusinessException(ErrorCodeEnum.USER_ROLE_NOT_FOUND);
                }
            }
            // 确定本次操作生效的角色：新角色优先: 否则沿用当前角色（避免因未传 roleId 而跳过业务规则校验）
            RoleEntity effectiveRole = newRole != null ? newRole
                    : (entity.getRoleId() != null ? roleService.getById(entity.getRoleId()) : null);
            // 确定本次操作生效的部门：新部门优先: 否则沿用当前部门
            Long effectiveDeptId = dto.getDeptId() != null ? dto.getDeptId() : entity.getDeptId();
            DeptEntity effectiveDept = effectiveDeptId != null ? deptService.getById(effectiveDeptId) : null;
            Long effectiveOrgId = dto.getOrgId() != null ? dto.getOrgId() : entity.getOrgId();
            if (effectiveRole != null) {
                // 角色业务规则校验（部门必填+类型匹配 + hospitals范围 + 设计师specialty）
                validateDeptRequired(effectiveRole, effectiveDept);
                validateHospitalScope(effectiveRole, dto.getHospitalIds());
                validateManagedOrgScope(effectiveRole, effectiveOrgId, dto.getManagedOrgIds());
                validateRegionalManagerDeptOrg(effectiveRole, effectiveDeptId, effectiveOrgId);
                validateSpecialty(effectiveRole, dto.getSpecialtyList());
            }
            // 生产人员角色校验加工中心，并复用查询结果更新冗余字段
            Long effectiveCenterId = dto.getCenterId() != null ? dto.getCenterId() : entity.getCenterId();
            if (effectiveRole != null && (RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(effectiveRole.getRoleCode())
                    || RoleCodeEnum.PRODUCTION_MANAGER.getCode().equals(effectiveRole.getRoleCode()))) {
                if (effectiveCenterId == null) {
                    log.warn("生产员角色必须绑定加工中心: roleId={}", effectiveRole.getId());
                    throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "加工中心");
                }
                com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity center =
                    processingCenterMapper.selectById(effectiveCenterId);
                if (center == null) {
                    log.warn("加工中心不存在: centerId={}", effectiveCenterId);
                    throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "加工中心");
                }
                if (dto.getCenterId() != null) {
                    entity.setCenterName(center.getCenterName());
                }
            }
            // 更新角色冗余字段: 并处理医院范围权限变更
            if (newRole != null) {
                entity.setRoleName(newRole.getRoleName());
                entity.setRoleCode(newRole.getRoleCode());
            }
            if (effectiveRole != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(effectiveRole.getDataScopeType())) {
                if (dto.getHospitalIds() != null && !dto.getHospitalIds().isEmpty()) {
                    // 覆盖式分配医院权限：先清空再写入: 保证与前端选择完全一致
                    userHospitalService.assignHospitals(id, dto.getHospitalIds());
                }
            } else if (newRole != null && !DataScopeTypeEnum.HOSPITALS.getCode().equals(newRole.getDataScopeType())) {
                // 角色从 HOSPITALS 切换为其他类型时: 清理旧的医院权限记录
                userHospitalService.assignHospitals(id, java.util.Collections.emptyList());
            }
            if (dto.getSpecialtyList() != null) {
                entity.setSpecialty(CollUtil.join(dto.getSpecialtyList(), ","));
            }

            // 更新用户信息（排除不允许通过此接口修改的字段）
            cn.hutool.core.bean.BeanUtil.copyProperties(dto, entity,
                    cn.hutool.core.bean.copier.CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setIgnoreProperties("id", "username", "password", "status",
                                    "createTime", "updateTime", "createBy", "updateBy",
                                    "managedOrgIds", "hospitalIds", "specialtyList"));
            // 空字符串工号统一置 null: 避免触发唯一函数索引冲突（NULL 不参与唯一约束）
            if (StrUtil.isBlank(entity.getEmployeeNo())) {
                entity.setEmployeeNo(null);
            }
            if (StrUtil.isBlank(entity.getEmail())) {
                entity.setEmail(null);
            }
            updateById(entity);
            if (effectiveRole != null && RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(effectiveRole.getRoleCode())) {
                if (dto.getManagedOrgIds() != null) {
                    userManagedOrgService.replaceManagedOrgIds(id, entity.getOrgId(), dto.getManagedOrgIds());
                } else if (!Objects.equals(originalOrgId, entity.getOrgId())) {
                    // 未提交额外机构表示保持原配置；主机构变化时仍需剔除与新主机构重复的旧关系。
                    userManagedOrgService.replaceManagedOrgIds(
                            id, entity.getOrgId(), userManagedOrgService.getManagedOrgIds(id));
                }
            } else if (newRole != null) {
                userManagedOrgService.replaceManagedOrgIds(id, entity.getOrgId(), Collections.emptyList());
            }
            log.info("更新用户成功: id={}", id);
        } catch (Exception e) {
            log.error("更新用户异常: id={}", id, e);
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
        log.info("删除用户: id={}", id);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 删除用户前先清理医院关联
        userHospitalService.assignHospitals(id, List.of());
        userManagedOrgService.replaceManagedOrgIds(id, entity.getOrgId(), Collections.emptyList());
        removeById(id);
        log.info("删除用户成功: id={}", id);
    }

    /**
     * 修改用户状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改用户状态: id={}, status={}", id, status);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        entity.setStatus(status);
        updateById(entity);
        // 禁用用户时: 强制踢出其当前会话: 使 token 立即失效
        if (StatusConstants.DISABLED == status) {
            try {
                cn.dev33.satoken.stp.StpUtil.kickout(id);
                log.info("已强制踢出用户会话: userId={}", id);
            } catch (Exception ex) {
                log.warn("踢出用户会话失败（用户可能未登录）: userId={}", id);
            }
        }
        log.info("修改用户状态成功: id={}, status={}", id, status);
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
        log.info("重置密码: userId={}", userId);
        UserEntity entity = getById(userId);
        if (entity == null) {
            log.warn("用户不存在: userId={}", userId);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 从系统配置获取默认密码（已内置兜底逻辑: 不会返回 null）
        String rawPassword = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
        entity.setPassword(passwordEncoder.encode(rawPassword));
        updateById(entity);
        log.info("重置密码成功: userId={}", userId);
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, ChangePasswordDTO dto) {
        log.info("修改密码: id={}", id);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 校验旧密码是否正确
        if (!passwordEncoder.matches(dto.getOldPassword(), entity.getPassword())) {
            log.warn("旧密码不正确: id={}", id);
            throw new BusinessException(ErrorCodeEnum.OLD_PASSWORD_ERROR);
        }
        // 校验新密码不能与旧密码相同
        if (passwordEncoder.matches(dto.getNewPassword(), entity.getPassword())) {
            log.warn("新密码与旧密码相同: id={}", id);
            throw new BusinessException(ErrorCodeEnum.NEW_PASSWORD_SAME_AS_OLD);
        }
        // 校验新密码强度
        if (!isPasswordStrong(dto.getNewPassword())) {
            log.warn("新密码强度不足: id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_PASSWORD_WEAK);
        }
        // 更新密码
        entity.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(entity);
        log.info("修改密码成功: id={}", id);
    }

    /**
     * 用户自更新（仅允许修改手机号和头像）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserBySelf(Long id, com.yigongbao.module.system.user.dto.UpdateUserBySelfDTO dto) {
        log.info("用户自更新信息: id={}", id);
        UserEntity entity = getById(id);
        if (entity == null) {
            log.warn("用户不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 只更新手机号和头像
        if (dto.getPhone() != null) {
            entity.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            entity.setAvatar(dto.getAvatar());
        }
        updateById(entity);
        log.info("用户自更新成功: id={}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验角色部门必填规则和部门类型匹配
     * <p>
     * 规则1：当角色 dataScopeType=DEPT 或 SELF 时: deptId 必填:
     * 规则2：角色的 accountType 必须与部门的 deptType 匹配（企业角色→企业部门: 业务角色→业务部门）:
     *
     * @param role 生效角色（null 时跳过校验）
     * @param dept 所选部门（null 时仅校验必填规则）
     */
    private void validateDeptRequired(RoleEntity role, DeptEntity dept) {
        if (role == null) {
            return;
        }
        String dataScopeType = role.getDataScopeType();
        // 规则1：dataScopeType为dept或self的角色必须选择部门
        if (DataScopeTypeEnum.DEPT.getCode().equals(dataScopeType)
                || DataScopeTypeEnum.SELF.getCode().equals(dataScopeType)) {
            if (dept == null) {
                log.warn("角色数据权限为{}: 但未选择部门: roleId={}", dataScopeType, role.getId());
                throw new BusinessException(ErrorCodeEnum.USER_DEPT_REQUIRED);
            }
        }
        // 规则2：如果选择了部门: 校验角色accountType与部门deptType是否匹配
        if (dept != null && role.getAccountType() != null && dept.getDeptType() != null) {
            if (!role.getAccountType().equals(dept.getDeptType())) {
                log.warn("角色账户类型与部门类型不匹配: roleAccountType={}, deptType={}",
                        role.getAccountType(), dept.getDeptType());
                throw new BusinessException(ErrorCodeEnum.USER_DEPT_TYPE_MISMATCH);
            }
        }
    }

    /**
     * 校验角色医院范围权限
     * <p>
     * 当角色 dataScopeType=HOSPITALS 时: hospitalIds 必填且每个 ID 必须是真实存在的医院机构
     * （orgType=ORG_TYPE_HOSPITAL）:其他数据范围类型直接跳过: 无需传医院列表:
     *
     * @param role        生效角色（null 时跳过校验）
     * @param hospitalIds 前端传入的医院 ID 列表
     */
    private void validateHospitalScope(RoleEntity role, List<Long> hospitalIds) {
        // 非医院范围角色无需校验: 直接返回
        if (role == null || !DataScopeTypeEnum.HOSPITALS.getCode().equals(role.getDataScopeType())) {
            return;
        }
        // 医院范围角色必须指定至少一家医院
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            log.warn("角色数据权限为医院范围: 但未指定医院: roleId={}", role.getId());
            throw new BusinessException(ErrorCodeEnum.USER_ROLE_HOSPITAL_SCOPE_REQUIRED);
        }
        // 批量查询并过滤出真实医院（orgType=ORG_TYPE_HOSPITAL）: 与传入列表取差集得到无效 ID
        Set<Long> existingIds = orgService.listByIds(hospitalIds).stream()
                .filter(org -> DictCodeConstants.ORG_TYPE_HOSPITAL.equals(org.getOrgType()))
                .map(OrgEntity::getId)
                .collect(Collectors.toSet());
        List<Long> invalidIds = hospitalIds.stream()
                .filter(hid -> !existingIds.contains(hid))
                .collect(Collectors.toList());
        if (!invalidIds.isEmpty()) {
            log.warn("存在无效的医院ID: invalidHospitalIds={}", invalidIds);
            throw new BusinessException(ErrorCodeEnum.USER_HOSPITAL_INVALID);
        }
    }

    /** 校验区域管理员的额外管理机构；仅允许正常的经销商和服务商。 */
    private void validateManagedOrgScope(RoleEntity role, Long primaryOrgId, List<Long> managedOrgIds) {
        boolean regionalManager = role != null
                && RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(role.getRoleCode());
        if (!regionalManager) {
            if (managedOrgIds != null && !managedOrgIds.isEmpty()) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "仅区域管理员可配置额外管理机构");
            }
            return;
        }
        if (primaryOrgId == null) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "主所属机构");
        }
        OrgEntity primaryOrg = orgService.getById(primaryOrgId);
        if (primaryOrg == null
                || !Integer.valueOf(StatusConstants.NORMAL).equals(primaryOrg.getStatus())
                || (!DictCodeConstants.ORG_TYPE_DEALER.equals(primaryOrg.getOrgType())
                    && !DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(primaryOrg.getOrgType()))) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "区域管理员主机构只能选择正常的经销商或服务商");
        }
        if (managedOrgIds == null || managedOrgIds.isEmpty()) return;
        List<Long> distinctIds = managedOrgIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(primaryOrgId))
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) return;
        List<OrgEntity> orgs = orgService.listByIds(distinctIds);
        Set<Long> validIds = orgs.stream()
                .filter(Objects::nonNull)
                .filter(org -> Integer.valueOf(StatusConstants.NORMAL).equals(org.getStatus()))
                .filter(org -> DictCodeConstants.ORG_TYPE_DEALER.equals(org.getOrgType())
                        || DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(org.getOrgType()))
                .map(OrgEntity::getId)
                .collect(Collectors.toSet());
        if (validIds.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "额外管理机构只能选择正常的经销商或服务商");
        }
    }

    /** 区域管理员的主机构必须属于其业务部门，避免通过编辑账户绕过创建时的组织约束。 */
    private void validateRegionalManagerDeptOrg(RoleEntity role, Long deptId, Long primaryOrgId) {
        if (role == null || !RoleCodeEnum.REGIONAL_MANAGER.getCode().equals(role.getRoleCode())) {
            return;
        }
        if (deptId == null || primaryOrgId == null) {
            return;
        }
        boolean belongsToDept = deptOrgMapper.selectList(
                        new LambdaQueryWrapper<DeptOrgEntity>()
                                .eq(DeptOrgEntity::getDeptId, deptId)
                                .eq(DeptOrgEntity::getOrgId, primaryOrgId))
                .stream()
                .anyMatch(relation -> Objects.equals(relation.getOrgId(), primaryOrgId));
        if (!belongsToDept) {
            log.warn("区域管理员主机构不属于部门: deptId={}, orgId={}", deptId, primaryOrgId);
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_BELONG_TO_DEPT);
        }
    }

    /**
     * 校验设计师专业方向
     * <p>
     * 仅对 designer / designer-manager 角色生效：至少选择一个方向: 且每个字典编码必须以
     * {@link com.yigongbao.common.constant.DictCodeConstants#USER_SPECIALTY} 为前缀并在字典表中存在:
     * 其他角色直接跳过: 无需传专业方向:
     *
     * @param role          生效角色（null 时跳过校验）
     * @param specialtyList 专业方向字典编码列表
     */
    private void validateSpecialty(RoleEntity role, List<String> specialtyList) {
        // 非设计师角色无需专业方向: 直接跳过
        if (role == null || role.getRoleCode() == null
                || !SPECIALTY_REQUIRED_ROLES.contains(role.getRoleCode())) {
            return;
        }
        // 设计师/设计师管理员必须至少选择一个专业方向
        if (CollUtil.isEmpty(specialtyList)) {
            log.warn("角色为设计师/设计师管理员: 但未指定专业方向: roleId={}", role.getId());
            throw new BusinessException(ErrorCodeEnum.USER_ROLE_SPECIALTY_REQUIRED);
        }
        String prefix = DictCodeConstants.USER_SPECIALTY + ".";
        for (String specialty : specialtyList) {
            // 校验编码格式：必须以 USER_SPECIALTY 前缀开头: 防止传入非专业方向字典值
            if (StrUtil.isBlank(specialty) || !specialty.startsWith(prefix)) {
                log.warn("专业方向字典编码无效: specialty={}", specialty);
                throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, prefix);
            }
            // 校验编码在字典表中真实存在: 防止传入已废弃或伪造的编码
            if (dictService.getByDictCode(specialty) == null) {
                log.warn("专业方向字典编码不存在: specialty={}", specialty);
                throw new BusinessException(ErrorCodeEnum.USER_SPECIALTY_INVALID, specialty);
            }
        }
    }

    /**
     * 校验生产员角色的加工中心绑定
     */
    private void validateProcessingCenter(RoleEntity role, Long centerId) {
        if (role == null || role.getRoleCode() == null) {
            return;
        }
        // 生产员角色必须绑定加工中心
        if (RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(role.getRoleCode())) {
            if (centerId == null) {
                log.warn("生产员角色必须绑定加工中心: roleId={}", role.getId());
                throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "加工中心");
            }
            // 校验加工中心是否存在
            com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity center =
                processingCenterMapper.selectById(centerId);
            if (center == null) {
                log.warn("加工中心不存在: centerId={}", centerId);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "加工中心");
            }
        }
    }

    /**
     * 填充用户实体冗余字段
     * <p>
     * 冗余字段（orgName/deptName/roleName/roleCode）存储在用户表中: 目的是避免列表查询时
     * 每行都 JOIN 三张关联表: 以空间换时间:复用调用方已查询的实体对象: 不再重复查库:
     *
     * @param entity     用户实体
     * @param orgEntity  已查询的机构实体（可为 null）
     * @param deptEntity 已查询的部门实体（可为 null）
     * @param roleEntity 已查询的角色实体（可为 null）
     */
    private void fillRedundantFields(UserEntity entity, OrgEntity orgEntity, DeptEntity deptEntity, RoleEntity roleEntity) {
        // 冗余机构名称: 避免查询时 JOIN sys_org
        if (entity.getOrgId() != null && orgEntity != null) {
            entity.setOrgName(orgEntity.getOrgName());
        }
        // 冗余部门名称: 避免查询时 JOIN sys_dept
        if (entity.getDeptId() != null && deptEntity != null) {
            entity.setDeptName(deptEntity.getDeptName());
        }
        // 冗余角色名称和编码: roleCode 用于前端权限判断: 避免查询时 JOIN sys_role
        if (entity.getRoleId() != null && roleEntity != null) {
            entity.setRoleName(roleEntity.getRoleName());
            entity.setRoleCode(roleEntity.getRoleCode());
        }
        // 冗余加工中心名称（仅生产员角色需要）
        if (entity.getCenterId() != null && roleEntity != null && RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(roleEntity.getRoleCode())) {
            com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity center =
                processingCenterMapper.selectById(entity.getCenterId());
            if (center != null) {
                entity.setCenterName(center.getCenterName());
            }
        }
    }

    /**
     * 校验密码强度
     * 规则：必须包含字母和数字: 长度6-20位
     *
     * @param password 密码
     * @return true-强度合格: false-强度不足
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
     * 保留首尾字符: 中间用星号替代（复用 Hutool StrUtil.hide）
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
            DictVO accountTypeDict = dictService.getByDictCode(vo.getAccountType());
            vo.setAccountTypeName(accountTypeDict != null ? accountTypeDict.getDictName() : "");
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
            // 结算类型父节点 dict_code=DictCodeConstants.SETTLEMENT_TYPE("8"): 其数据库 id=36
            DictEntity dictEntity = dictService.lambdaQuery()
                    .eq(DictEntity::getParentId, 36L)
                    .eq(DictEntity::getDictValue, vo.getSettlementType().toString())
                    .one();
            vo.setSettlementTypeName(dictEntity != null ? dictEntity.getDictName() : null);
        }
        // 填充收费模板名称
        if (vo.getChargingTemplateId() != null) {
            com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity template =
                    chargingTemplateService.getById(vo.getChargingTemplateId());
            vo.setChargingTemplateName(template != null ? template.getTemplateName() : null);
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
     * 校验真实姓名在同角色下是否存在
     */
    private boolean isRealNameExistsForRole(String realName, Long roleId) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getRealName, realName)
                .eq(UserEntity::getRoleId, roleId)) > 0;
    }

    /**
     * 校验真实姓名在同角色下是否存在（排除指定ID）
     */
    private boolean isRealNameExistsForRoleExcludingId(String realName, Long roleId, Long excludeId) {
        return count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getRealName, realName)
                .eq(UserEntity::getRoleId, roleId)
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

    @Override
    public List<Long> listUserIdsByCenterId(Long centerId) {
        if (centerId == null) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getCenterId, centerId)
                .eq(UserEntity::getStatus, StatusConstants.NORMAL)
                .eq(UserEntity::getIsDeleted, StatusConstants.NOT_DELETED))
                .stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
    }

    /**
     * 预览用户名（自动生成模式: 预占5分钟）
     *
     * @param orgId 机构ID
     * @return 预占的用户名: 手动模式返回 null
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
        log.info("预占用户名: orgId={}, username={}", orgId, username);
        return username;
    }

    /**
     * 按机构前缀生成用户名: 格式为 {prefix}{seq3位补零}（如 ceshi001）
     * <p>
     * 服务商机构若无独立前缀，使用生产企业统一前缀
     * </p>
     *
     * @param orgId 机构ID
     * @return 生成的用户名
     */
    private String generateUsername(Long orgId) {
        OrgEntity org = orgService.getById(orgId);
        if (org == null) {
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
        }

        String prefix = org.getUsernamePrefix();
        // 服务商机构无独立前缀时，使用生产企业的前缀
        if (StrUtil.isBlank(prefix) && DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(org.getOrgType())) {
            String manufacturerOrgIdStr = configService.getConfigValue(
                SystemConfigKeyEnum.MANUFACTURER_ORG_ID.getKey());
            if (StrUtil.isNotBlank(manufacturerOrgIdStr)) {
                OrgEntity manufacturer = orgService.getById(Long.parseLong(manufacturerOrgIdStr));
                if (manufacturer != null) {
                    prefix = manufacturer.getUsernamePrefix();
                }
            }
        }

        if (StrUtil.isBlank(prefix)) {
            throw new BusinessException(ErrorCodeEnum.ORG_USERNAME_PREFIX_MISSING);
        }

        // generateWithSeqSuffix 返回格式为 "prefix-N": 解析后格式化为 "prefixNNN"
        String raw = codeGeneratorService.generateWithSeqSuffix(CodeRuleConstants.USER_NO, prefix);
        int dashIdx = raw.lastIndexOf('-');
        String prefixPart = raw.substring(0, dashIdx);
        long seq = Long.parseLong(raw.substring(dashIdx + 1));
        return prefixPart + String.format("%03d", seq);
    }

    @Override
    public void exportUsers(HttpServletResponse response) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserEntity::getCreateTime).last("LIMIT 10000");

        List<UserEntity> list = list(wrapper);
        List<UserVO> voList = list.stream().map(this::toVOWithNames).collect(Collectors.toList());

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            var sheet = workbook.createSheet("用户列表");
            String[] headers = {"用户名", "姓名", "手机号", "角色", "所属机构", "所属部门", "状态", "创建时间"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 4000);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (int i = 0; i < voList.size(); i++) {
                UserVO vo = voList.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(vo.getUsername());
                row.createCell(1).setCellValue(vo.getRealName());
                row.createCell(2).setCellValue(vo.getPhone());
                row.createCell(3).setCellValue(vo.getRoleName());
                row.createCell(4).setCellValue(vo.getOrgName());
                row.createCell(5).setCellValue(vo.getDeptName());
                row.createCell(6).setCellValue(vo.getStatusName());
                row.createCell(7).setCellValue(vo.getCreateTime() != null ? vo.getCreateTime().format(formatter) : "");
            }

            String filename = URLEncoder.encode("账户导出.xlsx", StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            workbook.write(response.getOutputStream());
            log.info("导出用户列表: 总数={}", voList.size());
        } catch (IOException e) {
            log.error("导出用户列表失败", e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public List<Long> getUserIdsByRoleCode(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return Collections.emptyList();
        }

        List<UserEntity> users = baseMapper.selectList(
                new LambdaQueryWrapper<UserEntity>()
                        .select(UserEntity::getId)
                        .eq(UserEntity::getRoleCode, roleCode)
                        .eq(UserEntity::getStatus, StatusConstants.NORMAL)
        );

        return users.stream()
                .map(UserEntity::getId)
                .collect(Collectors.toList());
    }

    @Override
    public String getUserRealName(Long userId) {
        if (userId == null) {
            return null;
        }

        UserEntity user = baseMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .select(UserEntity::getRealName)
                        .eq(UserEntity::getId, userId)
        );
        return user != null ? user.getRealName() : null;
    }

    @Override
    public String getCurrentUserRoleCode() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            UserEntity user = baseMapper.selectOne(
                    new LambdaQueryWrapper<UserEntity>()
                            .select(UserEntity::getRoleCode)
                            .eq(UserEntity::getId, userId)
            );
            return user != null ? user.getRoleCode() : null;
        } catch (Exception e) {
            log.warn("获取当前用户角色失败，用户未登录或会话已过期");
            return null;
        }
    }
}
