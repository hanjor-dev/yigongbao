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
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import com.yigongbao.module.system.dept.mapper.DeptOrgMapper;
import com.yigongbao.module.system.dept.mapper.DeptMapper;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dept.vo.DeptVO;
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

import java.util.List;
import java.util.Objects;
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

    private final OrgService orgService;
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
        log.info("分页查询部门列表，dto={}", dto);
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
            IPage<DeptVO> voPage = pageResult.convert(this::toVOWithNames);
            log.info("分页查询部门列表成功，总数={}", pageResult.getTotal());
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
        log.info("根据ID查询部门详情，id={}", id);
        try {
            DeptEntity entity = getById(id);
            if (entity == null) {
                log.warn("部门不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            DeptVO vo = toVOWithNames(entity);
            log.info("查询部门详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
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
            log.info("创建部门成功，id={}, deptCode={}", entity.getId(), deptCode);
        } catch (BusinessException e) {
            throw e;
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
            Integer deptType = dto.getDeptType() != null ? dto.getDeptType() : entity.getDeptType();
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
            log.info("更新部门成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
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
                log.warn("部门不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 部门下存在用户时禁止删除，避免用户失去归属
            if (hasUsers(id)) {
                log.warn("该部门下存在用户，无法删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_HAS_USERS);
            }
            // 级联物理删除 sys_dept_org 关联记录，防止产生孤儿数据
            deptOrgMapper.delete(new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getDeptId, id));
            // 对部门本身执行逻辑删除（is_deleted=1）
            removeById(id);
            log.info("删除部门成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
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
                log.warn("部门不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 更新状态
            entity.setStatus(status);
            updateById(entity);
            log.info("修改部门状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
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
        log.info("全量查询部门列表，orgId={}", orgId);
        try {
            List<DeptEntity> entityList;
            if (orgId != null) {
                // 查询关联了指定机构的所有部门
                List<Long> deptIds = deptOrgMapper.selectList(
                        new LambdaQueryWrapper<DeptOrgEntity>().eq(DeptOrgEntity::getOrgId, orgId))
                        .stream().map(DeptOrgEntity::getDeptId).collect(Collectors.toList());
                if (deptIds.isEmpty()) return List.of();
                entityList = listByIds(deptIds);
            } else {
                entityList = list(new LambdaQueryWrapper<DeptEntity>().orderByAsc(DeptEntity::getDeptName));
            }
            return entityList.stream().map(this::toVOWithNames).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("全量查询部门列表异常", e);
            throw e;
        }
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
        // 填充负责人姓名（每条记录额外查询一次用户表）
        if (vo.getLeaderUserId() != null) {
            UserEntity userEntity = userService.getById(vo.getLeaderUserId());
            if (userEntity != null) vo.setLeaderUserName(userEntity.getRealName());
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
     * - 内部部门（deptType=1）：orgIds 只能传一个，且必须是生产企业
     * - 外部部门（deptType=2）：每个经销商只能属于一个外部部门（排除 excludeDeptId 自身）
     *
     * @param orgIds        待关联的机构ID列表
     * @param deptType      部门类型（1=内部，2=外部）
     * @param excludeDeptId 更新时排除自身，创建时传 null
     */
    private void validateOrgTypeMatchDeptType(List<Long> orgIds, Integer deptType, Long excludeDeptId) {
        if (deptType == 1) {
            // 内部部门只能关联一个生产企业
            if (orgIds.size() > 1) {
                throw new BusinessException(ErrorCodeEnum.DEPT_INTERNAL_ORG_LIMIT);
            }
            List<OrgEntity> orgs = orgService.listByIds(orgIds);
            boolean mismatch = orgs.stream().anyMatch(o -> !DictCodeConstants.ORG_TYPE_PRODUCER.equals(o.getOrgType()));
            if (mismatch) {
                throw new BusinessException(ErrorCodeEnum.ORG_DEPT_TYPE_MISMATCH);
            }
        } else {
            // 外部部门：校验机构类型均为经销商
            List<OrgEntity> orgs = orgService.listByIds(orgIds);
            boolean mismatch = orgs.stream().anyMatch(o -> !DictCodeConstants.ORG_TYPE_DEALER.equals(o.getOrgType()));
            if (mismatch) {
                throw new BusinessException(ErrorCodeEnum.ORG_DEPT_TYPE_MISMATCH);
            }
            // 校验每个经销商未被其他外部部门关联（一个经销商只属于一个外部部门）
            for (Long orgId : orgIds) {
                LambdaQueryWrapper<DeptOrgEntity> wrapper = new LambdaQueryWrapper<DeptOrgEntity>()
                        .eq(DeptOrgEntity::getOrgId, orgId);
                if (excludeDeptId != null) {
                    wrapper.ne(DeptOrgEntity::getDeptId, excludeDeptId);
                }
                if (deptOrgMapper.selectCount(wrapper) > 0) {
                    throw new BusinessException(ErrorCodeEnum.DEPT_ORG_ALREADY_BOUND);
                }
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
