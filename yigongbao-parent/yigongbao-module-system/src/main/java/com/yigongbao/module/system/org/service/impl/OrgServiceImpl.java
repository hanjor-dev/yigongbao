package com.yigongbao.module.system.org.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.area.entity.AreaEntity;
import com.yigongbao.module.basic.area.service.AreaService;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import com.yigongbao.module.system.org.convert.OrgConvert;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.OrgPageDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.entity.OrgHospitalEntity;
import com.yigongbao.module.system.org.mapper.OrgHospitalMapper;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.hospitalGroupTemplate.mapper.HospitalGroupTemplateDetailMapper;
import com.yigongbao.module.system.hospitalGroupTemplate.entity.HospitalGroupTemplateDetailEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.org.vo.OrgVO;
import com.yigongbao.module.system.org.vo.OrgHospitalChangeCheckVO;
import com.yigongbao.module.system.org.vo.OrgOperationCheckVO;
import com.yigongbao.module.system.doctor.service.DoctorService;
import com.yigongbao.module.system.doctor.vo.DoctorVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import com.yigongbao.module.system.dept.mapper.DeptOrgMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.entity.UserHospitalEntity;
import com.yigongbao.module.system.user.mapper.UserHospitalMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 机构 Service 实现类
 * 处理机构相关的业务逻辑，包括机构CRUD、状态管理等
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrgServiceImpl extends ServiceImpl<OrgMapper, OrgEntity> implements OrgService {

    private final DictService dictService;
    private final UserMapper userMapper;
    private final com.yigongbao.module.system.config.service.ConfigService configService;
    private final UserHospitalMapper userHospitalMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final AreaService areaService;
    private final OrgHospitalMapper orgHospitalMapper;
    private final HospitalGroupTemplateDetailMapper templateDetailMapper;
    private final com.yigongbao.module.system.hospitalGroupTemplate.mapper.HospitalGroupTemplateMapper templateMapper;
    private final DeptOrgMapper deptOrgMapper;
    private final FileService fileService;
    private final com.yigongbao.module.system.dept.mapper.DeptMapper deptMapper;

    @Autowired
    @Lazy
    private DoctorService doctorService;

    /**
     * 分页查询机构列表
     *
     * @param dto 分页查询参数
     * @return 分页后的机构列表
     */
    @Override
    public IPage<OrgVO> listOrg(OrgPageDTO dto) {
        // 如果未传入分页参数，使用默认值
        int pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
        int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
        Page<OrgEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OrgEntity> wrapper = new LambdaQueryWrapper<>();
        // 固定排除生产企业（内置，不对外展示）和其他医院（占位，不对外展示）
        String unknownHospitalIdStr = configService.getConfigValue(SystemConfigKeyEnum.UNKNOWN_HOSPITAL_ORG_ID.getKey());
        wrapper.ne(OrgEntity::getOrgType, DictCodeConstants.ORG_TYPE_PRODUCER);
        if (unknownHospitalIdStr != null) {
            wrapper.ne(OrgEntity::getId, Long.parseLong(unknownHospitalIdStr));
        }
        wrapper.like(StrUtil.isNotBlank(dto.getOrgName()), OrgEntity::getOrgName, dto.getOrgName())
                .eq(StrUtil.isNotBlank(dto.getOrgType()), OrgEntity::getOrgType, dto.getOrgType())
                .eq(Objects.nonNull(dto.getAreaId()), OrgEntity::getAreaId, dto.getAreaId())
                .eq(Objects.nonNull(dto.getStatus()), OrgEntity::getStatus, dto.getStatus())
                .orderByDesc(OrgEntity::getCreateTime);
        // 执行分页查询
        IPage<OrgEntity> pageResult = page(page, wrapper);

        // 批量查询字典，避免循环中逐条查询（N+1 问题）
        Map<String, String> dictNameMap = Collections.emptyMap();
        List<OrgEntity> records = pageResult.getRecords();
        if (!records.isEmpty()) {
            // 收集所有需要的字典编码（去重）
            Set<String> dictCodes = records.stream()
                    .flatMap(org -> Stream.of(org.getOrgType(), org.getHospitalLevel(), org.getHospitalType()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            // 批量查询字典并构建 Map（dictCode -> dictName）
            if (!dictCodes.isEmpty()) {
                dictNameMap = dictCodes.stream()
                        .map(dictService::getByDictCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(DictVO::getDictCode, DictVO::getDictName));
            }
        }

        // 转换为VO并填充字典名称
        Map<String, String> finalDictMap = dictNameMap;
        return pageResult.convert(entity -> toVOWithDictNames(entity, finalDictMap));
    }

    /**
     * 根据ID查询机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    @Override
    public OrgVO getOrgById(Long id) {
        OrgEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
        }
        OrgVO vo = toVOWithDictNames(entity);
        // 填充经销商关联的医疗机构
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(entity.getOrgType())) {
            List<Long> hospitalOrgIds = orgHospitalMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrgHospitalEntity>()
                            .eq(OrgHospitalEntity::getDistributorOrgId, id))
                    .stream().map(OrgHospitalEntity::getHospitalOrgId).collect(Collectors.toList());
            assert vo != null;
            vo.setHospitalOrgIds(hospitalOrgIds);
            if (!hospitalOrgIds.isEmpty()) {
                List<OrgEntity> hospitals = listByIds(hospitalOrgIds);
                vo.setHospitalOrgNames(hospitals.stream().map(OrgEntity::getOrgName).collect(Collectors.toList()));
            }
        }
        // 填充资质文件详细信息
        if (StrUtil.isNotBlank(entity.getQualificationFile())) {
            vo.setQualificationFileInfo(fileService.getById(entity.getQualificationFile()));
        }
        return vo;
    }

    /**
     * 创建机构
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrg(CreateOrgDTO dto) {
        // 禁止创建生产企业类型
        if (DictCodeConstants.ORG_TYPE_PRODUCER.equals(dto.getOrgType())) {
            throw new BusinessException(ErrorCodeEnum.ORG_TYPE_NOT_ALLOWED);
        }
        // 校验机构名称是否已存在
        if (isOrgNameExists(dto.getOrgName())) {
            log.warn("机构名称已存在: orgName={}", dto.getOrgName());
            throw new BusinessException(ErrorCodeEnum.ORG_EXISTS);
        }
        // 校验账号前缀唯一性（生产企业/经销商专用）
        if (StrUtil.isNotBlank(dto.getUsernamePrefix()) && isUsernamePrefixExists(dto.getUsernamePrefix())) {
            log.warn("账号前缀已存在: usernamePrefix={}", dto.getUsernamePrefix());
            throw new BusinessException(ErrorCodeEnum.ORG_USERNAME_PREFIX_EXISTS);
        }
        // 校验机构类型是否存在
        if (!isOrgTypeValid(dto.getOrgType())) {
            log.warn("机构类型不存在: orgType={}", dto.getOrgType());
            throw new BusinessException(ErrorCodeEnum.ORG_TYPE_NOT_FOUND);
        }
        // 经销商机构类型时账号前缀必填
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(dto.getOrgType()) && StrUtil.isBlank(dto.getUsernamePrefix())) {
            throw new BusinessException(ErrorCodeEnum.ORG_USERNAME_PREFIX_REQUIRED);
        }
        // 经销商机构类型时资质类型必填
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(dto.getOrgType()) && dto.getQualificationType() == null) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "资质类型");
        }
        // 经销商类型 + 医疗器械资质时资质文件必填
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(dto.getOrgType())
                && Integer.valueOf(1).equals(dto.getQualificationType())
                && StrUtil.isBlank(dto.getQualificationFile())) {
            throw new BusinessException(ErrorCodeEnum.ORG_CERT_FILE_REQUIRED);
        }
        if (StrUtil.isNotBlank(dto.getQualificationFile())) {
            FileVO fileVO = fileService.getById(dto.getQualificationFile());
            if (fileVO == null) {
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
        }
        // 生成机构编码
        String prefix = getOrgPrefixByType(dto.getOrgType());
        String orgCode = codeGeneratorService.generateWithCustomPrefix(CodeRuleConstants.ORG_NO, prefix);
        // DTO转换为实体对象
        OrgEntity entity = OrgConvert.toEntity(dto);
        entity.setOrgCode(orgCode);
        entity.setStatus(StatusConstants.NORMAL);
        // 根据 areaId 查询地区名称并写入冗余字段
        if (dto.getAreaId() != null) {
            AreaEntity areaEntity = areaService.getById(dto.getAreaId());
            entity.setAreaName(areaEntity != null ? areaEntity.getName() : null);
        }
        // 插入数据库
        save(entity);
        // 资质文件关联到机构
        if (StrUtil.isNotBlank(dto.getQualificationFile())) {
            fileService.linkFile(dto.getQualificationFile(), FileBizTypeEnum.ORG_CERT.getDictCode(), entity.getId());
        }
        // 经销商类型：保存关联医疗机构
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(dto.getOrgType()) && dto.getHospitalOrgIds() != null && !dto.getHospitalOrgIds().isEmpty()) {
            // 校验 hospitalOrgIds 中的机构必须是医疗机构类型
            List<OrgEntity> hospitals = listByIds(dto.getHospitalOrgIds());
            boolean hasInvalid = hospitals.stream().anyMatch(o -> !DictCodeConstants.ORG_TYPE_HOSPITAL.equals(o.getOrgType()));
            if (hasInvalid || hospitals.size() != dto.getHospitalOrgIds().size()) {
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            }
            saveOrgHospitalRelations(entity.getId(), dto.getHospitalOrgIds());
        }
        log.info("创建机构: id={}, orgCode={}, orgName={}, orgType={}",
            entity.getId(), orgCode, dto.getOrgName(), dto.getOrgType());
    }

    /**
     * 更新机构
     *
     * @param id  机构ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrg(Long id, UpdateOrgDTO dto) {
        OrgEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
        }
        // 禁止修改机构类型
        if (dto.getOrgType() != null && !dto.getOrgType().equals(entity.getOrgType())) {
            throw new BusinessException(ErrorCodeEnum.ORG_TYPE_NOT_ALLOWED);
        }
        // 机构名称有变更时才校验唯一性，避免与自身冲突
        if (StrUtil.isNotBlank(dto.getOrgName()) && !dto.getOrgName().equals(entity.getOrgName())) {
            if (isOrgNameExistsExcludingId(dto.getOrgName(), id)) {
                throw new BusinessException(ErrorCodeEnum.ORG_EXISTS);
            }
        }
        // 账号前缀不允许修改，如果传入且与原值不同则拒绝
        if (StrUtil.isNotBlank(dto.getUsernamePrefix()) && !dto.getUsernamePrefix().equals(entity.getUsernamePrefix())) {
            throw new BusinessException(ErrorCodeEnum.ORG_USERNAME_PREFIX_NOT_ALLOWED);
        }
        // 资质类型以入参为准，入参为空则沿用原值，防止局部更新时误清空
        Integer qualType = dto.getQualificationType() != null ? dto.getQualificationType() : entity.getQualificationType();
        String qualFile = StrUtil.isNotBlank(dto.getQualificationFile()) ? dto.getQualificationFile() : entity.getQualificationFile();
        // 经销商类型 + 医疗器械资质时资质文件必填
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(entity.getOrgType())
                && Integer.valueOf(1).equals(qualType)
                && StrUtil.isBlank(qualFile)) {
            throw new BusinessException(ErrorCodeEnum.ORG_CERT_FILE_REQUIRED);
        }
        if (StrUtil.isNotBlank(dto.getQualificationFile())) {
            FileVO fileVO = fileService.getById(dto.getQualificationFile());
            if (fileVO == null) {
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
        }
        // 记录原机构名称，用于后续判断是否需要同步 sys_user.org_name
        String originalOrgName = entity.getOrgName();
        // 排除不可变字段，将DTO属性覆盖到实体（orgCode、usernamePrefix、orgType 不可修改）
        BeanUtils.copyProperties(dto, entity, "id", "orgCode", "usernamePrefix", "orgType", "createTime", "updateTime", "createBy", "updateBy", "hospitalOrgIds");
        // areaId变更时同步刷新冗余的地区名称字段
        if (dto.getAreaId() != null) {
            AreaEntity areaEntity = areaService.getById(dto.getAreaId());
            entity.setAreaName(areaEntity != null ? areaEntity.getName() : null);
        }
        updateById(entity);
        // 资质文件有变更时重新关联
        if (StrUtil.isNotBlank(dto.getQualificationFile())) {
            fileService.linkFile(dto.getQualificationFile(), FileBizTypeEnum.ORG_CERT.getDictCode(), id);
        }
        // 机构名称变更时，同步更新 sys_user 中的冗余字段 org_name
        if (StrUtil.isNotBlank(dto.getOrgName()) && !dto.getOrgName().equals(originalOrgName)) {
            userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserEntity>()
                    .eq(UserEntity::getOrgId, id)
                    .set(UserEntity::getOrgName, dto.getOrgName()));
        }
        // 经销商类型且前端传入了 hospitalOrgIds 时，全量替换关联关系（先校验后删插）
        String orgType = entity.getOrgType();
        if (DictCodeConstants.ORG_TYPE_DEALER.equals(orgType) && dto.getHospitalOrgIds() != null) {
            // 先校验新列表合法性，再执行写操作
            if (!dto.getHospitalOrgIds().isEmpty()) {
                List<OrgEntity> hospitals = listByIds(dto.getHospitalOrgIds());
                boolean hasInvalid = hospitals.stream().anyMatch(o -> !DictCodeConstants.ORG_TYPE_HOSPITAL.equals(o.getOrgType()));
                if (hasInvalid || hospitals.size() != dto.getHospitalOrgIds().size()) {
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
                }
            }
            // 校验通过后再删除旧关联
            orgHospitalMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrgHospitalEntity>()
                    .eq(OrgHospitalEntity::getDistributorOrgId, id));
            if (!dto.getHospitalOrgIds().isEmpty()) {
                saveOrgHospitalRelations(id, dto.getHospitalOrgIds());
            }
            // 同步清理该经销商下业务员中已失效的医院权限
            java.util.Set<Long> newSet = new java.util.HashSet<>(dto.getHospitalOrgIds());
            List<UserEntity> users = userMapper.selectList(
                    new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOrgId, id));
            for (UserEntity user : users) {
                List<Long> toRemove = userHospitalMapper.selectHospitalIdsByUserId(user.getId())
                        .stream().filter(hid -> !newSet.contains(hid)).collect(Collectors.toList());
                if (!toRemove.isEmpty()) {
                    userHospitalMapper.delete(new LambdaQueryWrapper<UserHospitalEntity>()
                            .eq(UserHospitalEntity::getUserId, user.getId())
                            .in(UserHospitalEntity::getHospitalId, toRemove));
                    log.info("清理用户失效医院权限: userId={}, removedCount={}", user.getId(), toRemove.size());
                }
            }
        }
        log.info("更新机构: id={}, orgName={}", id, entity.getOrgName());
    }

    /**
     * 删除机构
     *
     * @param id 机构ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrg(Long id) {
        // 校验机构是否存在
        OrgEntity entity = getById(id);
        if (entity == null) {
            log.warn("机构不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
        }
        // 校验该机构下是否有用户
        if (hasUsers(id)) {
            log.warn("机构下存在用户: 无法删除: id={}", id);
            throw new BusinessException(ErrorCodeEnum.ORG_HAS_USERS);
        }
        // 清理经销商-医疗机构关联记录（作为经销商或医疗机构均需清理）
        orgHospitalMapper.delete(new LambdaQueryWrapper<OrgHospitalEntity>()
                .eq(OrgHospitalEntity::getDistributorOrgId, id)
                .or()
                .eq(OrgHospitalEntity::getHospitalOrgId, id));
        // 清理用户-医院关联记录（该机构作为医疗机构被分配给用户的记录）
        userHospitalMapper.delete(new LambdaQueryWrapper<UserHospitalEntity>()
                .eq(UserHospitalEntity::getHospitalId, id));
        // 清理医院组合模板明细记录（该机构作为医院被加入模板的记录）
        templateDetailMapper.delete(new LambdaQueryWrapper<HospitalGroupTemplateDetailEntity>()
                .eq(HospitalGroupTemplateDetailEntity::getHospitalId, id));
        // 清理部门-机构关联记录（该机构被部门关联的记录）
        deptOrgMapper.delete(new LambdaQueryWrapper<DeptOrgEntity>()
                .eq(DeptOrgEntity::getOrgId, id));
        // 逻辑删除
        removeById(id);
        log.info("删除机构: id={}, orgName={}", id, entity.getOrgName());
    }

    /**
     * 修改机构状态
     *
     * @param id     机构ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        // 校验机构是否存在
        OrgEntity entity = getById(id);
        if (entity == null) {
            log.warn("机构不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
        }
        Integer oldStatus = entity.getStatus();
        // 更新状态
        entity.setStatus(status);
        updateById(entity);
        // 禁用机构时踢出其下所有用户的登录会话
        if (StatusConstants.DISABLED == status) {
            List<UserEntity> users = userMapper.selectList(
                    new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOrgId, id));
            int kickedCount = 0;
            for (UserEntity u : users) {
                try {
                    cn.dev33.satoken.stp.StpUtil.kickout(u.getId());
                    kickedCount++;
                } catch (Exception ex) {
                    log.warn("踢出用户会话失败（用户可能未登录）: userId={}", u.getId());
                }
            }
            log.info("修改机构状态: id={}, {} -> {}, 踢出会话={}个", id, oldStatus, status, kickedCount);
        } else {
            log.info("修改机构状态: id={}, {} -> {}", id, oldStatus, status);
        }
    }

    /**
     * 全量查询机构列表（用于前端下拉选择）
     *
     * @return 机构列表（包含字典名称）
     */
    @Override
    public List<OrgVO> listAllOrg() {
        LambdaQueryWrapper<OrgEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgEntity::getStatus, StatusConstants.NORMAL);
        wrapper.ne(OrgEntity::getOrgType, DictCodeConstants.ORG_TYPE_PRODUCER);
        String unknownHospitalIdStr = configService.getConfigValue(SystemConfigKeyEnum.UNKNOWN_HOSPITAL_ORG_ID.getKey());
        if (unknownHospitalIdStr != null) {
            wrapper.ne(OrgEntity::getId, Long.parseLong(unknownHospitalIdStr));
        }
        wrapper.orderByAsc(OrgEntity::getOrgName);
        List<OrgEntity> entityList = list(wrapper);

        // 查询所有已被经销商关联的医疗机构ID集合
        Set<Long> boundHospitalIds = orgHospitalMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .map(OrgHospitalEntity::getHospitalOrgId)
                .collect(Collectors.toSet());

        // 查询所有已被部门关联的经销商ID集合
        Set<Long> boundDealerIds = deptOrgMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .map(DeptOrgEntity::getOrgId)
                .collect(Collectors.toSet());

        return entityList.stream()
                .map(entity -> {
                    OrgVO vo = toVOWithDictNames(entity);
                    // 如果是医疗机构类型，设置是否已被关联标识
                    if (DictCodeConstants.ORG_TYPE_HOSPITAL.equals(vo.getOrgType())) {
                        vo.setIsBound(boundHospitalIds.contains(vo.getId()));
                    }
                    // 如果是经销商类型，设置是否已被关联标识
                    if (DictCodeConstants.ORG_TYPE_DEALER.equals(vo.getOrgType())) {
                        vo.setIsBound(boundDealerIds.contains(vo.getId()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为VO并填充字典名称
     *
     * @param entity 机构实体
     * @return 机构VO
     */
    /**
     * 转换为VO并填充字典名称（使用字典Map，避免重复查询）
     *
     * @param entity 机构实体
     * @param dictNameMap 字典Map（dictCode -> dictName）
     * @return 机构VO
     */
    private OrgVO toVOWithDictNames(OrgEntity entity, Map<String, String> dictNameMap) {
        OrgVO vo = OrgConvert.toVO(entity);
        if (vo == null) return null;
        if (vo.getOrgType() != null) {
            vo.setOrgTypeName(dictNameMap.get(vo.getOrgType()));
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        if (vo.getHospitalLevel() != null) {
            vo.setHospitalLevelName(dictNameMap.get(vo.getHospitalLevel()));
        }
        if (vo.getHospitalType() != null) {
            vo.setHospitalTypeName(dictNameMap.get(vo.getHospitalType()));
        }
        return vo;
    }

    /**
     * 转换为VO并填充字典名称（单条查询场景）
     *
     * @param entity 机构实体
     * @return 机构VO
     */
    private OrgVO toVOWithDictNames(OrgEntity entity) {
        OrgVO vo = OrgConvert.toVO(entity);
        if (vo == null) return null;
        if (vo.getOrgType() != null) {
            DictVO dict = dictService.getByDictCode(vo.getOrgType());
            vo.setOrgTypeName(dict != null ? dict.getDictName() : null);
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        if (vo.getHospitalLevel() != null) {
            DictVO dict = dictService.getByDictCode(vo.getHospitalLevel());
            vo.setHospitalLevelName(dict != null ? dict.getDictName() : null);
        }
        if (vo.getHospitalType() != null) {
            DictVO dict = dictService.getByDictCode(vo.getHospitalType());
            vo.setHospitalTypeName(dict != null ? dict.getDictName() : null);
        }
        return vo;
    }

    /**
     * 保存经销商与医疗机构的关联关系
     * <p>将经销商ID与多个医疗机构ID逐条写入关联表</p>
     *
     * @param distributorOrgId 经销商机构ID
     * @param hospitalOrgIds   关联的医疗机构ID列表
     */
    private void saveOrgHospitalRelations(Long distributorOrgId, List<Long> hospitalOrgIds) {
        List<OrgHospitalEntity> relations = hospitalOrgIds.stream().map(hospitalOrgId -> {
            OrgHospitalEntity rel = new OrgHospitalEntity();
            rel.setDistributorOrgId(distributorOrgId);
            rel.setHospitalOrgId(hospitalOrgId);
            return rel;
        }).collect(Collectors.toList());
        relations.forEach(orgHospitalMapper::insert);
        log.info("保存经销商关联医院: distributorOrgId={}, 关联数={}", distributorOrgId, relations.size());
    }

    /**
     * 校验机构名称是否存在
     *
     * @param orgName 机构名称
     * @return true-存在，false-不存在
     */
    private boolean isOrgNameExists(String orgName) {
        return count(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgName, orgName)) > 0;
    }

    private boolean isUsernamePrefixExists(String usernamePrefix) {
        return count(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getUsernamePrefix, usernamePrefix)) > 0;
    }

    /**
     * 校验机构名称是否存在（排除指定ID）
     *
     * @param orgName 机构名称
     * @param excludeId 排除的机构ID
     * @return true-存在，false-不存在
     */
    private boolean isOrgNameExistsExcludingId(String orgName, Long excludeId) {
        return count(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgName, orgName)
                .ne(OrgEntity::getId, excludeId)) > 0;
    }

    /**
     * 校验机构类型是否有效
     *
     * @param orgType 机构类型（字典编码）
     * @return true-有效，false-无效
     */
    private boolean isOrgTypeValid(String orgType) {
        if (orgType == null) {
            return false;
        }
        return dictService.getByDictCode(orgType) != null;
    }

    /**
     * 根据机构类型获取编码前缀
     *
     * @param orgType 机构类型（字典编码）
     * @return 编码前缀
     */
    private String getOrgPrefixByType(String orgType) {
        return switch (orgType) {
            case DictCodeConstants.ORG_TYPE_PRODUCER -> "ORG-P-";  // 生产企业
            case DictCodeConstants.ORG_TYPE_DEALER -> "ORG-D-";  // 经销商
            case DictCodeConstants.ORG_TYPE_HOSPITAL -> "ORG-H-";  // 医疗机构
            default -> "ORG-O-";       // 其他
        };
    }

    /**
     * 校验该机构下是否有用户
     *
     * @param orgId 机构ID
     * @return true-有用户，false-无用户
     */
    private boolean hasUsers(Long orgId) {
        return userMapper.countByOrgId(orgId) > 0;
    }

    /**
     * 预检查删除机构的影响
     * 返回该机构下的用户列表，以及（医疗机构时）关联的医生列表
     *
     * @param id 机构ID
     * @return 检查结果，affected=true 时需前端提示用户确认
     */
    @Override
    public OrgOperationCheckVO checkRemove(Long id) {
        OrgEntity entity = getById(id);
        if (entity == null) throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);

        List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOrgId, id));
        List<OrgOperationCheckVO.AffectedUserVO> affectedUsers = users.stream().map(u -> {
            OrgOperationCheckVO.AffectedUserVO vo = new OrgOperationCheckVO.AffectedUserVO();
            vo.setId(u.getId());
            vo.setRealName(u.getRealName());
            return vo;
        }).collect(Collectors.toList());

        List<OrgOperationCheckVO.AffectedDoctorVO> affectedDoctors = new java.util.ArrayList<>();
        if (DictCodeConstants.ORG_TYPE_HOSPITAL.equals(entity.getOrgType())) {
            affectedDoctors = doctorService.listByHospitalId(id).stream().map(d -> {
                OrgOperationCheckVO.AffectedDoctorVO vo = new OrgOperationCheckVO.AffectedDoctorVO();
                vo.setId(d.getId());
                vo.setDoctorName(d.getDoctorName());
                return vo;
            }).collect(Collectors.toList());
        }

        // 查询关联该机构的部门（删除机构时会解除关联）
        List<Long> deptIds = deptOrgMapper.selectList(
                new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getOrgId, id))
                .stream().map(DeptOrgEntity::getDeptId).collect(Collectors.toList());
        List<OrgOperationCheckVO.AffectedDeptVO> affectedDepts = deptIds.isEmpty()
                ? java.util.Collections.emptyList()
                : deptMapper.selectBatchIds(deptIds).stream().map(d -> {
                    OrgOperationCheckVO.AffectedDeptVO vo = new OrgOperationCheckVO.AffectedDeptVO();
                    vo.setId(d.getId());
                    vo.setDeptName(d.getDeptName());
                    return vo;
                }).collect(Collectors.toList());

        // 查询包含该医院的组合模板（仅医疗机构类型需要检查）
        List<OrgOperationCheckVO.AffectedTemplateVO> affectedTemplates = java.util.Collections.emptyList();
        if (DictCodeConstants.ORG_TYPE_HOSPITAL.equals(entity.getOrgType())) {
            List<Long> templateIds = templateDetailMapper.selectList(
                    new LambdaQueryWrapper<HospitalGroupTemplateDetailEntity>()
                            .eq(HospitalGroupTemplateDetailEntity::getHospitalId, id))
                    .stream().map(HospitalGroupTemplateDetailEntity::getTemplateId)
                    .distinct().collect(Collectors.toList());
            if (!templateIds.isEmpty()) {
                affectedTemplates = templateMapper.selectBatchIds(templateIds).stream()
                        .filter(t -> t != null)
                        .map(t -> {
                            OrgOperationCheckVO.AffectedTemplateVO vo = new OrgOperationCheckVO.AffectedTemplateVO();
                            vo.setId(t.getId());
                            vo.setTemplateName(t.getTemplateName());
                            return vo;
                        }).collect(Collectors.toList());
            }
        }

        OrgOperationCheckVO result = new OrgOperationCheckVO();
        result.setAffectedUsers(affectedUsers);
        result.setAffectedDoctors(affectedDoctors);
        result.setAffectedDepts(affectedDepts);
        result.setAffectedTemplates(affectedTemplates);
        result.setAffected(!affectedUsers.isEmpty() || !affectedDoctors.isEmpty()
                || !affectedDepts.isEmpty() || !affectedTemplates.isEmpty());
        if (result.isAffected()) {
            StringBuilder msg = new StringBuilder("删除该机构将产生以下影响：");
            if (!affectedUsers.isEmpty()) msg.append("【").append(affectedUsers.size()).append(" 个用户账号将失去机构归属】");
            if (!affectedDoctors.isEmpty()) msg.append("【").append(affectedDoctors.size()).append(" 位医生历史记录将失效】");
            if (!affectedDepts.isEmpty()) msg.append("【").append(affectedDepts.size()).append(" 个部门将解除与该机构的关联】");
            if (!affectedTemplates.isEmpty()) msg.append("【").append(affectedTemplates.size()).append(" 个医院组合模板将移除该医院】");
            msg.append("，请确认是否继续？");
            result.setMessage(msg.toString());
        }
        return result;
    }

    /**
     * 预检查禁用机构的影响
     * 返回该机构下的用户列表（禁用后这些用户将被踢出会话且无法再登录）
     *
     * @param id 机构ID
     * @return 检查结果，affected=true 时需前端提示用户确认
     */
    @Override
    public OrgOperationCheckVO checkDisable(Long id) {
        OrgEntity entity = getById(id);
        if (entity == null) throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);

        List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOrgId, id));
        List<OrgOperationCheckVO.AffectedUserVO> affectedUsers = users.stream().map(u -> {
            OrgOperationCheckVO.AffectedUserVO vo = new OrgOperationCheckVO.AffectedUserVO();
            vo.setId(u.getId());
            vo.setRealName(u.getRealName());
            return vo;
        }).collect(Collectors.toList());

        // 查询包含该医院的组合模板（仅医疗机构类型需要检查）
        List<OrgOperationCheckVO.AffectedTemplateVO> affectedTemplates = java.util.Collections.emptyList();
        if (DictCodeConstants.ORG_TYPE_HOSPITAL.equals(entity.getOrgType())) {
            List<Long> templateIds = templateDetailMapper.selectList(
                    new LambdaQueryWrapper<HospitalGroupTemplateDetailEntity>()
                            .eq(HospitalGroupTemplateDetailEntity::getHospitalId, id))
                    .stream().map(HospitalGroupTemplateDetailEntity::getTemplateId)
                    .distinct().collect(Collectors.toList());
            if (!templateIds.isEmpty()) {
                affectedTemplates = templateMapper.selectBatchIds(templateIds).stream()
                        .filter(t -> t != null)
                        .map(t -> {
                            OrgOperationCheckVO.AffectedTemplateVO vo = new OrgOperationCheckVO.AffectedTemplateVO();
                            vo.setId(t.getId());
                            vo.setTemplateName(t.getTemplateName());
                            return vo;
                        }).collect(Collectors.toList());
            }
        }

        OrgOperationCheckVO result = new OrgOperationCheckVO();
        result.setAffectedUsers(affectedUsers);
        result.setAffectedDoctors(java.util.Collections.emptyList());
        result.setAffectedTemplates(affectedTemplates);
        result.setAffected(!affectedUsers.isEmpty() || !affectedTemplates.isEmpty());
        if (result.isAffected()) {
            StringBuilder msg = new StringBuilder("禁用该机构将产生以下影响：");
            if (!affectedUsers.isEmpty()) msg.append("【").append(affectedUsers.size()).append(" 个用户将被踢出会话且无法登录】");
            if (!affectedTemplates.isEmpty()) msg.append("【").append(affectedTemplates.size()).append(" 个医院组合模板中该医院将暂时不可用】");
            msg.append("，请确认是否继续？");
            result.setMessage(msg.toString());
        }
        return result;
    }

    /**
     * 预检查经销商关联医院变更对用户权限的影响
     */
    @Override
    public OrgHospitalChangeCheckVO checkHospitalChange(Long id, List<Long> newHospitalIds) {
        OrgHospitalChangeCheckVO result = new OrgHospitalChangeCheckVO();

        // 查询当前关联的医院ID列表
        List<Long> currentHospitalIds = orgHospitalMapper.selectList(
                new LambdaQueryWrapper<OrgHospitalEntity>()
                        .eq(OrgHospitalEntity::getDistributorOrgId, id))
                .stream().map(OrgHospitalEntity::getHospitalOrgId).collect(Collectors.toList());

        // 计算被移除的医院
        java.util.Set<Long> newSet = newHospitalIds == null ? java.util.Collections.emptySet() : new java.util.HashSet<>(newHospitalIds);
        List<Long> removedIds = currentHospitalIds.stream().filter(hid -> !newSet.contains(hid)).collect(Collectors.toList());

        if (removedIds.isEmpty()) {
            result.setAffected(false);
            return result;
        }

        // 查询被移除医院的名称
        List<OrgHospitalChangeCheckVO.RemovedHospitalVO> removedHospitals = listByIds(removedIds).stream().map(org -> {
            OrgHospitalChangeCheckVO.RemovedHospitalVO vo = new OrgHospitalChangeCheckVO.RemovedHospitalVO();
            vo.setId(org.getId());
            vo.setOrgName(org.getOrgName());
            return vo;
        }).collect(Collectors.toList());

        // 查询该经销商下所有用户，找出拥有被移除医院权限的用户
        List<UserEntity> users = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getOrgId, id));
        List<OrgHospitalChangeCheckVO.AffectedUserVO> affectedUsers = new java.util.ArrayList<>();
        for (UserEntity user : users) {
            List<Long> userHospitalIds = userHospitalMapper.selectHospitalIdsByUserId(user.getId());
            List<Long> affected = userHospitalIds.stream().filter(removedIds::contains).collect(Collectors.toList());
            if (!affected.isEmpty()) {
                OrgHospitalChangeCheckVO.AffectedUserVO vo = new OrgHospitalChangeCheckVO.AffectedUserVO();
                vo.setId(user.getId());
                vo.setRealName(user.getRealName());
                vo.setRemovedHospitalIds(affected);
                affectedUsers.add(vo);
            }
        }

        result.setAffected(!affectedUsers.isEmpty());
        result.setRemovedHospitals(removedHospitals);
        result.setAffectedUsers(affectedUsers);
        if (result.isAffected()) {
            result.setMessage("本次变更将移除 " + removedHospitals.size() + " 家医院，导致 " + affectedUsers.size() + " 个业务员失去对应医院的访问权限，请确认是否继续？");
        }
        return result;
    }
}
