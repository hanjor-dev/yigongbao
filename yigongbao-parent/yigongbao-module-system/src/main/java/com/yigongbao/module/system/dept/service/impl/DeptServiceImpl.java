package com.yigongbao.module.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.system.dept.convert.DeptConvert;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.DeptPageDTO;
import com.yigongbao.module.system.dept.dto.DeptStatisticsQueryDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import com.yigongbao.module.system.dept.mapper.DeptOrgMapper;
import com.yigongbao.module.system.dept.mapper.DeptMapper;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dept.vo.DeptVO;
import com.yigongbao.module.system.dept.vo.DeptStatisticsVO;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
 * 部门 Service 实现类
 * 处理部门相关的业务逻辑，包括部门CRUD、状态管理等
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeptServiceImpl extends ServiceImpl<DeptMapper, DeptEntity> implements DeptService {

    @Override
    public DeptStatisticsVO getStatistics(DeptStatisticsQueryDTO dto) {
        LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeptEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                .like(dto != null && StrUtil.isNotBlank(dto.getDeptName()), DeptEntity::getDeptName,
                        dto == null ? null : dto.getDeptName())
                .eq(dto != null && dto.getStatus() != null, DeptEntity::getStatus,
                        dto == null ? null : dto.getStatus());
        DeptStatisticsVO result = getBaseMapper().selectStatistics(wrapper);
        return result == null ? new DeptStatisticsVO() : result;
    }

    private final OrgService orgService;
    private final DictService dictService;
    private final CodeGeneratorService codeGeneratorService;
    private final DeptOrgMapper deptOrgMapper;

    @Autowired
    @Lazy
    private UserService userService;

    /**
     * 分页查询部门列表
     *
     * @param dto 分页查询参数
     * @return 分页后的部门列表
     */
    @Override
    public IPage<DeptVO> listDept(DeptPageDTO dto) {
        try {
            // 如果未传入分页参数，使用默认值
            int pageNum = dto.getPageNum() != null && dto.getPageNum() > 0 ? dto.getPageNum() : 1;
            int pageSize = dto.getPageSize() != null && dto.getPageSize() > 0 ? dto.getPageSize() : 10;
            Page<DeptEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Objects.nonNull(dto.getDeptType()), DeptEntity::getDeptType, dto.getDeptType())
                    .like(StrUtil.isNotBlank(dto.getDeptName()), DeptEntity::getDeptName, dto.getDeptName())
                    .eq(Objects.nonNull(dto.getStatus()), DeptEntity::getStatus, dto.getStatus())
                    .orderByDesc(DeptEntity::getCreateTime);
            IPage<DeptEntity> pageResult = page(page, wrapper);

            // 批量查询字典和关联机构，避免循环中逐条查询（N+1 问题）
            Map<String, String> dictNameMap = Collections.emptyMap();
            Map<Long, List<OrgEntity>> deptOrgMap = Collections.emptyMap();
            List<DeptEntity> records = pageResult.getRecords();
            if (!records.isEmpty()) {
                // 收集所有需要的字典编码（去重）
                Set<String> dictCodes = records.stream()
                        .map(DeptEntity::getDeptType)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                // 批量查询字典并构建 Map（dictCode -> dictName）
                if (!dictCodes.isEmpty()) {
                    dictNameMap = dictCodes.stream()
                            .map(dictService::getByDictCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(DictVO::getDictCode, DictVO::getDictName));
                }
                // 批量查询所有部门的关联机构（一次查询替代N次查询）
                List<Long> deptIds = records.stream().map(DeptEntity::getId).collect(Collectors.toList());
                List<DeptOrgEntity> deptOrgList = deptOrgMapper.selectList(
                        new LambdaQueryWrapper<DeptOrgEntity>().in(DeptOrgEntity::getDeptId, deptIds));
                if (!deptOrgList.isEmpty()) {
                    // 收集所有机构ID并批量查询机构信息
                    Set<Long> orgIds = deptOrgList.stream()
                            .map(DeptOrgEntity::getOrgId)
                            .collect(Collectors.toSet());
                    Map<Long, OrgEntity> orgMap = orgService.listByIds(new ArrayList<>(orgIds)).stream()
                            .collect(Collectors.toMap(OrgEntity::getId, Function.identity()));
                    // 构建 Map<deptId, List<OrgEntity>>
                    deptOrgMap = deptOrgList.stream()
                            .collect(Collectors.groupingBy(
                                    DeptOrgEntity::getDeptId,
                                    Collectors.mapping(
                                            rel -> orgMap.get(rel.getOrgId()),
                                            Collectors.filtering(Objects::nonNull, Collectors.toList())
                                    )
                            ));
                }
            }

            // 转换为VO并填充字典名称和关联机构
            Map<String, String> finalDictMap = dictNameMap;
            Map<Long, List<OrgEntity>> finalDeptOrgMap = deptOrgMap;
            IPage<DeptVO> voPage = pageResult.convert(entity -> toVOWithNames(entity, finalDictMap, finalDeptOrgMap));
            return voPage;
        } catch (Exception e) {
            log.error("分页查询部门列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询部门详情
     *
     * @param id 部门ID
     * @return 部门详情
     */
    @Override
    public DeptVO getDeptById(Long id) {
        try {
            DeptEntity entity = getById(id);
            if (entity == null) {
                log.warn("部门不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            DeptVO vo = toVOWithNames(entity);
            return vo;
        } catch (Exception e) {
            log.error("查询部门详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建部门
     * <p>
     * 校验部门名称唯一性及关联机构类型合法性后，生成部门编码并持久化，
     * 最后写入 sys_dept_org 关联关系。
     *
     * @param dto 创建部门请求参数，包含部门名称、类型及关联机构列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(CreateDeptDTO dto) {
        log.info("创建部门，deptName={}, deptType={}", dto.getDeptName(), dto.getDeptType());
        try {
            // 校验部门名称全局唯一，防止重名
            if (isDeptNameExists(dto.getDeptName())) {
                throw new BusinessException(ErrorCodeEnum.DEPT_EXISTS);
            }
            // 有关联机构时，校验机构类型与部门类型匹配（内部↔生产企业，外部↔经销商）
            if (dto.getOrgIds() != null && !dto.getOrgIds().isEmpty()) {
                validateOrgTypeMatchDeptType(dto.getOrgIds(), dto.getDeptType(), null);
            }
            // 通过编码规则生成唯一部门编号
            String deptCode = codeGeneratorService.generate(CodeRuleConstants.DEPT_NO);
            DeptEntity entity = DeptConvert.toEntity(dto);
            entity.setDeptCode(deptCode);
            // 新建部门默认为启用状态
            entity.setStatus(StatusConstants.NORMAL);
            save(entity);
            // 写入 sys_dept_org 关联，建立部门与机构的多对多关系
            if (dto.getOrgIds() != null && !dto.getOrgIds().isEmpty()) {
                saveDeptOrgRelations(entity.getId(), dto.getOrgIds());
            }
            log.info("创建部门: id={}, deptCode={}", entity.getId(), deptCode);
        } catch (Exception e) {
            log.error("创建部门异常，deptName={}", dto.getDeptName(), e);
            throw e;
        }
    }

    /**
     * 更新部门信息
     * <p>
     * 支持部分字段更新；若传入 orgIds，则以先删后插方式全量替换关联机构。
     *
     * @param id  部门ID
     * @param dto 更新请求参数，orgIds 为 null 表示不修改关联机构，空列表表示清空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long id, UpdateDeptDTO dto) {
        log.info("更新部门，id={}", id);
        try {
            DeptEntity entity = getById(id);
            if (entity == null) {
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 仅在名称发生变更时才校验唯一性，避免无意义的重复查询
            if (StrUtil.isNotBlank(dto.getDeptName()) && !dto.getDeptName().equals(entity.getDeptName())) {
                if (isDeptNameExistsExcludingId(dto.getDeptName(), id)) {
                    throw new BusinessException(ErrorCodeEnum.DEPT_EXISTS);
                }
            }
            // 若未传入新 deptType，则沿用原值，用于后续机构类型匹配校验
            String deptType = dto.getDeptType() != null ? dto.getDeptType() : entity.getDeptType();
            if (dto.getOrgIds() != null && !dto.getOrgIds().isEmpty()) {
                validateOrgTypeMatchDeptType(dto.getOrgIds(), deptType, id);
            } else if (dto.getDeptType() != null && !dto.getDeptType().equals(entity.getDeptType()) && dto.getOrgIds() == null) {
                // deptType 发生变更但未传 orgIds：校验现有关联机构与新类型是否兼容
                List<Long> existingOrgIds = deptOrgMapper.selectList(
                        new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getDeptId, id))
                        .stream().map(DeptOrgEntity::getOrgId).collect(Collectors.toList());
                if (!existingOrgIds.isEmpty()) {
                    validateOrgTypeMatchDeptType(existingOrgIds, dto.getDeptType(), id);
                }
            }
            // 排除不可变字段及 orgIds（关联关系单独处理）
            BeanUtils.copyProperties(dto, entity, "id", "deptCode", "createTime", "updateTime", "createBy", "updateBy", "orgIds");
            updateById(entity);
            // 部门名称变更时，同步更新 sys_user 中的冗余字段 dept_name
            if (StrUtil.isNotBlank(dto.getDeptName()) && !dto.getDeptName().equals(entity.getDeptName())) {
                userService.lambdaUpdate()
                        .eq(UserEntity::getDeptId, id)
                        .set(UserEntity::getDeptName, dto.getDeptName())
                        .update();
            }
            // 关联机构采用先删后插策略，保证数据一致性；orgIds=null 时跳过，不影响现有关联
            if (dto.getOrgIds() != null) {
                deptOrgMapper.delete(new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getDeptId, id));
                if (!dto.getOrgIds().isEmpty()) {
                    saveDeptOrgRelations(id, dto.getOrgIds());
                }
            }
            log.info("更新部门: id={}", id);
        } catch (Exception e) {
            log.error("更新部门异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除部门
     * <p>
     * 删除前校验部门存在性及是否有在职用户；
     * 物理清除 sys_dept_org 关联记录后再对部门做逻辑删除。
     *
     * @param id 部门ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDept(Long id) {
        log.info("删除部门，id={}", id);
        try {
            DeptEntity entity = getById(id);
            if (entity == null) {
                log.warn("部门不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 部门下存在用户时禁止删除，避免用户失去归属
            if (hasUsers(id)) {
                log.warn("该部门下存在用户，无法删除: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_HAS_USERS);
            }
            // 级联物理删除 sys_dept_org 关联记录，防止产生孤儿数据
            deptOrgMapper.delete(new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getDeptId, id));
            // 对部门本身执行逻辑删除（is_deleted=1）
            removeById(id);
            log.info("删除部门: id={}", id);
        } catch (Exception e) {
            log.error("删除部门异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改部门状态
     *
     * @param id     部门ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改部门状态，id={}, status={}", id, status);
        try {
            // 校验部门是否存在
            DeptEntity entity = getById(id);
            if (entity == null) {
                log.warn("部门不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 更新状态
            entity.setStatus(status);
            updateById(entity);
            log.info("修改部门状态: id={}, status={}", id, status);
        } catch (Exception e) {
            log.error("修改部门状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    /**
     * 全量查询部门列表（用于前端下拉选择）
     *
     * @param orgId 机构ID（非必填，传入则只查询该机构下的部门）
     * @return 部门列表（包含关联名称）
     */
    @Override
    public List<DeptVO> listAllDept(Long orgId) {
        try {
            List<DeptEntity> entityList;
            if (orgId != null) {
                // 查询关联了指定机构的所有部门
                List<Long> deptIds = deptOrgMapper.selectList(
                        new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getOrgId, orgId))
                        .stream().map(DeptOrgEntity::getDeptId).collect(Collectors.toList());
                if (deptIds.isEmpty()) return List.of();
                entityList = list(new LambdaQueryWrapper<DeptEntity>()
                        .in(DeptEntity::getId, deptIds)
                        .eq(DeptEntity::getStatus, StatusConstants.NORMAL));
            } else {
                entityList = list(new LambdaQueryWrapper<DeptEntity>()
                        .eq(DeptEntity::getStatus, StatusConstants.NORMAL)
                        .orderByAsc(DeptEntity::getDeptName));
            }
            return entityList.stream().map(this::toVOWithNames).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("全量查询部门列表异常", e);
            throw e;
        }
    }

    /**
     * 根据部门ID查询关联机构列表
     *
     * @param id 部门ID
     * @return 关联机构列表
     */
    @Override
    public List<DeptVO.OrgSimpleVO> listOrgsByDeptId(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
        }
        List<Long> orgIds = deptOrgMapper.selectList(
                new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getDeptId, id))
                .stream().map(DeptOrgEntity::getOrgId).collect(Collectors.toList());
        if (orgIds.isEmpty()) return List.of();
        return orgService.listByIds(orgIds).stream().map(o -> {
            DeptVO.OrgSimpleVO s = new DeptVO.OrgSimpleVO();
            s.setId(o.getId());
            s.setOrgName(o.getOrgName());
            s.setOrgCode(o.getOrgCode());
            s.setOrgType(o.getOrgType());
            return s;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 将部门实体转换为 VO，并填充关联机构名称、状态名称、负责人姓名。
     * <p>
     * 注意：此方法在分页/列表场景下会对每条记录各发起 1~3 次额外查询（N+1 问题），
     * 数据量较大时需考虑批量预加载优化。
     *
     * @param entity 部门实体
     * @return 填充了关联名称的部门 VO，entity 为 null 时返回 null
     */
    /**
     * 转换为VO并填充关联名称（使用Map，避免重复查询）
     *
     * @param entity 部门实体
     * @param dictNameMap 字典Map（dictCode -> dictName）
     * @param deptOrgMap 部门关联机构Map（deptId -> List<OrgEntity>）
     * @return 部门VO
     */
    private DeptVO toVOWithNames(DeptEntity entity, Map<String, String> dictNameMap, Map<Long, List<OrgEntity>> deptOrgMap) {
        DeptVO vo = DeptConvert.toVO(entity);
        if (vo == null) return null;
        // 从 Map 中获取关联机构
        List<OrgEntity> orgs = deptOrgMap.getOrDefault(entity.getId(), Collections.emptyList());
        if (!orgs.isEmpty()) {
            vo.setOrgs(orgs.stream().map(o -> {
                DeptVO.OrgSimpleVO s = new DeptVO.OrgSimpleVO();
                s.setId(o.getId());
                s.setOrgName(o.getOrgName());
                s.setOrgCode(o.getOrgCode());
                s.setOrgType(o.getOrgType());
                return s;
            }).collect(Collectors.toList()));
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        if (vo.getDeptType() != null) {
            vo.setDeptTypeName(dictNameMap.getOrDefault(vo.getDeptType(), ""));
        }
        return vo;
    }

    /**
     * 转换为VO并填充关联名称（单条查询场景）
     *
     * @param entity 部门实体
     * @return 部门VO
     */
    private DeptVO toVOWithNames(DeptEntity entity) {
        DeptVO vo = DeptConvert.toVO(entity);
        if (vo == null) return null;
        // 查询该部门关联的所有机构ID（每条记录单独查询，存在 N+1 风险）
        List<Long> orgIds = deptOrgMapper.selectList(
                new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getDeptId, entity.getId()))
                .stream().map(DeptOrgEntity::getOrgId).collect(Collectors.toList());
        if (!orgIds.isEmpty()) {
            List<OrgEntity> orgs = orgService.listByIds(orgIds);
            vo.setOrgs(orgs.stream().map(o -> {
                DeptVO.OrgSimpleVO s = new DeptVO.OrgSimpleVO();
                s.setId(o.getId());
                s.setOrgName(o.getOrgName());
                s.setOrgCode(o.getOrgCode());
                s.setOrgType(o.getOrgType());
                return s;
            }).collect(Collectors.toList()));
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        if (vo.getDeptType() != null) {
            DictVO dictVO = dictService.getByDictCode(vo.getDeptType());
            vo.setDeptTypeName(dictVO != null ? dictVO.getDictName() : "");
        }
        return vo;
    }

    /**
     * 判断指定部门名称是否已存在（全局范围）。
     *
     * @param deptName 部门名称
     * @return true 表示已存在同名部门
     */
    private boolean isDeptNameExists(String deptName) {
        return count(new LambdaQueryWrapper<DeptEntity>().eq(DeptEntity::getDeptName, deptName)) > 0;
    }

    /**
     * 判断指定部门名称是否已被其他部门使用（排除自身）。
     *
     * @param deptName  部门名称
     * @param excludeId 排除的部门ID（当前编辑的部门）
     * @return true 表示已有其他部门使用该名称
     */
    private boolean isDeptNameExistsExcludingId(String deptName, Long excludeId) {
        return count(new LambdaQueryWrapper<DeptEntity>()
                .eq(DeptEntity::getDeptName, deptName)
                .ne(DeptEntity::getId, excludeId)) > 0;
    }

    /**
     * 判断指定部门下是否存在用户。
     *
     * @param deptId 部门ID
     * @return true 表示该部门下有用户
     */
    private boolean hasUsers(Long deptId) {
        return userService.countByDeptId(deptId) > 0;
    }

    /**
     * 校验机构类型与部门类型的匹配关系，并附加以下约束：
     * - 企业部门（deptType=6.1）：允许生产企业（最多1个）或服务商（可多个），不允许混合
     * - 业务部门（deptType=6.2）：每个经销商/服务商只能属于一个业务部门（排除 excludeDeptId 自身）
     *
     * @param orgIds        待关联的机构ID列表
     * @param deptType      部门类型（字典编码：6.1=企业部门，6.2=业务部门）
     * @param excludeDeptId 更新时排除自身，创建时传 null
     */
    private void validateOrgTypeMatchDeptType(List<Long> orgIds, String deptType, Long excludeDeptId) {
        if (StatusConstants.DEPT_TYPE_ENTERPRISE.equals(deptType)) {
            List<OrgEntity> orgs = orgService.listByIds(orgIds);
            // 收集机构类型
            Set<String> orgTypes = orgs.stream()
                .map(OrgEntity::getOrgType)
                .filter(type -> DictCodeConstants.ORG_TYPE_PRODUCER.equals(type)
                    || DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(type))
                .collect(Collectors.toSet());
            // 不允许混合生产企业和服务商
            if (orgTypes.size() > 1) {
                log.warn("企业部门不允许混合生产企业和服务商: orgTypes={}", String.join(",", orgTypes));
                throw new BusinessException(ErrorCodeEnum.DEPT_ORG_TYPE_MIXED);
            }
            // 校验机构类型必须是生产企业或服务商
            boolean mismatch = orgs.stream().anyMatch(o ->
                !DictCodeConstants.ORG_TYPE_PRODUCER.equals(o.getOrgType())
                && !DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(o.getOrgType()));
            if (mismatch) {
                log.warn("企业部门关联的机构类型错误: orgTypes={}",
                    orgs.stream().map(OrgEntity::getOrgType).collect(Collectors.joining(",")));
                throw new BusinessException(ErrorCodeEnum.DEPT_ENTERPRISE_ORG_TYPE_ERROR);
            }
            // 如果包含生产企业，只能关联1个
            long producerCount = orgs.stream()
                .filter(o -> DictCodeConstants.ORG_TYPE_PRODUCER.equals(o.getOrgType()))
                .count();
            if (producerCount > 1) {
                throw new BusinessException(ErrorCodeEnum.DEPT_INTERNAL_ORG_LIMIT);
            }
        } else if (StatusConstants.DEPT_TYPE_BUSINESS.equals(deptType)) {
            // 业务部门：关联机构不允许混合经销商和服务商类型
            List<OrgEntity> orgs = orgService.listByIds(orgIds);
            Set<String> orgTypes = orgs.stream()
                .map(OrgEntity::getOrgType)
                .filter(type -> DictCodeConstants.ORG_TYPE_DEALER.equals(type)
                    || DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(type))
                .collect(Collectors.toSet());
            if (orgTypes.size() > 1) {
                log.warn("业务部门关联机构类型混合: orgTypes={}", String.join(",", orgTypes));
                throw new BusinessException(ErrorCodeEnum.DEPT_ORG_TYPE_MIXED);
            }
            // 校验机构类型均为经销商或服务商
            boolean mismatch = orgs.stream().anyMatch(o ->
                !DictCodeConstants.ORG_TYPE_DEALER.equals(o.getOrgType())
                && !DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(o.getOrgType()));
            if (mismatch) {
                throw new BusinessException(ErrorCodeEnum.ORG_DEPT_TYPE_MISMATCH);
            }
            // 校验每个经销商/服务商未被其他业务部门关联（一个机构只属于一个部门）
            List<Long> boundOrgIds = new ArrayList<>();
            for (Long orgId : orgIds) {
                LambdaQueryWrapper<DeptOrgEntity> wrapper = new LambdaQueryWrapper<DeptOrgEntity>()
                        .eq(DeptOrgEntity::getOrgId, orgId);
                if (excludeDeptId != null) {
                    wrapper.ne(DeptOrgEntity::getDeptId, excludeDeptId);
                }
                if (deptOrgMapper.selectCount(wrapper) > 0) {
                    boundOrgIds.add(orgId);
                }
            }
            if (!boundOrgIds.isEmpty()) {
                String orgNames = orgService.listByIds(boundOrgIds).stream()
                        .map(OrgEntity::getOrgName)
                        .collect(Collectors.joining("、"));
                throw new BusinessException(ErrorCodeEnum.DEPT_ORG_ALREADY_BOUND, orgNames);
            }
        }
    }

    /**
     * 批量写入部门与机构的关联记录到 sys_dept_org 表。
     * <p>
     * 逐条插入，调用方需确保在事务内执行以保证原子性。
     *
     * @param deptId 部门ID
     * @param orgIds 待关联的机构ID列表
     */
    private void saveDeptOrgRelations(Long deptId, List<Long> orgIds) {
        // 逐条插入关联记录（数据量可控，无需批量优化）
        orgIds.forEach(orgId -> {
            DeptOrgEntity rel = new DeptOrgEntity();
            rel.setDeptId(deptId);
            rel.setOrgId(orgId);
            deptOrgMapper.insert(rel);
        });
    }
}
