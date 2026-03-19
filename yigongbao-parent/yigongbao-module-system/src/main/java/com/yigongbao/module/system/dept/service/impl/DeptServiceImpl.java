package com.yigongbao.module.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.dept.convert.DeptConvert;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.mapper.DeptMapper;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dept.vo.DeptVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

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
    private final UserMapper userMapper;

    /**
     * 分页查询部门列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param orgId    所属机构ID
     * @param deptName 部门名称（模糊查询）
     * @param status   状态
     * @return 分页后的部门列表
     */
    @Override
    public IPage<DeptVO> listDept(Integer pageNum, Integer pageSize, Long orgId, String deptName, Integer status) {
        log.info("分页查询部门列表，pageNum={}, pageSize={}, orgId={}, deptName={}, status={}",
                pageNum, pageSize, orgId, deptName, status);
        try {
            // 构建分页对象
            Page<DeptEntity> page = new Page<>(pageNum, pageSize);
            // 构建查询条件
            LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Objects.nonNull(orgId), DeptEntity::getOrgId, orgId)
                    .like(StringUtils.hasText(deptName), DeptEntity::getDeptName, deptName)
                    .eq(Objects.nonNull(status), DeptEntity::getStatus, status)
                    .orderByDesc(DeptEntity::getCreateTime);
            // 执行分页查询
            IPage<DeptEntity> pageResult = page(page, wrapper);
            // 转换为VO并填充关联名称
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
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDept(CreateDeptDTO dto) {
        log.info("创建部门，deptName={}, orgId={}", dto.getDeptName(), dto.getOrgId());
        try {
            // 校验所属机构是否存在
            OrgEntity orgEntity = orgService.getById(dto.getOrgId());
            if (orgEntity == null) {
                log.warn("所属机构不存在，orgId={}", dto.getOrgId());
                throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
            }
            // 校验部门名称是否已存在（同一机构下唯一）
            if (isDeptNameExists(dto.getDeptName(), dto.getOrgId())) {
                log.warn("部门名称已存在，deptName={}, orgId={}", dto.getDeptName(), dto.getOrgId());
                throw new BusinessException(ErrorCodeEnum.DEPT_EXISTS);
            }
            // 生成部门编码
            String deptCode = generateDeptCode();
            // DTO转换为实体对象
            DeptEntity entity = DeptConvert.toEntity(dto);
            entity.setDeptCode(deptCode);
            entity.setStatus(StatusConstants.NORMAL);
            // 插入数据库
            save(entity);
            log.info("创建部门成功，id={}, deptCode={}", entity.getId(), deptCode);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建部门异常，deptName={}", dto.getDeptName(), e);
            throw e;
        }
    }

    /**
     * 更新部门
     *
     * @param id  部门ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Long id, UpdateDeptDTO dto) {
        log.info("更新部门，id={}", id);
        try {
            // 校验部门是否存在
            DeptEntity entity = getById(id);
            if (entity == null) {
                log.warn("部门不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 校验部门名称是否与其他部门重复（同一机构下唯一）
            if (StringUtils.hasText(dto.getDeptName()) && !dto.getDeptName().equals(entity.getDeptName())) {
                if (isDeptNameExistsExcludingId(dto.getDeptName(), entity.getOrgId(), id)) {
                    log.warn("部门名称已存在，deptName={}", dto.getDeptName());
                    throw new BusinessException(ErrorCodeEnum.DEPT_EXISTS);
                }
            }
            // 更新部门信息
            BeanUtils.copyProperties(dto, entity, "id", "deptCode", "orgId", "createTime", "updateTime", "createBy", "updateBy");
            // 更新数据库
            updateById(entity);
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
     *
     * @param id 部门ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDept(Long id) {
        log.info("删除部门，id={}", id);
        try {
            // 校验部门是否存在
            DeptEntity entity = getById(id);
            if (entity == null) {
                log.warn("部门不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_NOT_FOUND);
            }
            // 校验该部门下是否有用户
            if (hasUsers(id)) {
                log.warn("该部门下存在用户，无法删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DEPT_HAS_USERS);
            }
            // 逻辑删除
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

    // ==================== 私有方法 ====================

    /**
     * 转换为VO并填充关联名称
     *
     * @param entity 部门实体
     * @return 部门VO
     */
    private DeptVO toVOWithNames(DeptEntity entity) {
        DeptVO vo = DeptConvert.toVO(entity);
        if (vo == null) {
            return null;
        }
        // 填充机构名称
        if (vo.getOrgId() != null) {
            OrgEntity orgEntity = orgService.getById(vo.getOrgId());
            if (orgEntity != null) {
                vo.setOrgName(orgEntity.getOrgName());
            }
        }
        // 填充状态名称
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        // 填充部门负责人姓名
        if (vo.getLeaderUserId() != null) {
            UserEntity userEntity = userMapper.selectById(vo.getLeaderUserId());
            if (userEntity != null) {
                vo.setLeaderUserName(userEntity.getRealName());
            }
        }
        return vo;
    }

    /**
     * 校验部门名称是否存在
     *
     * @param deptName 部门名称
     * @param orgId    所属机构ID
     * @return true-存在，false-不存在
     */
    private boolean isDeptNameExists(String deptName, Long orgId) {
        return count(new LambdaQueryWrapper<DeptEntity>()
                .eq(DeptEntity::getDeptName, deptName)
                .eq(DeptEntity::getOrgId, orgId)) > 0;
    }

    /**
     * 校验部门名称是否存在（排除指定ID）
     *
     * @param deptName  部门名称
     * @param orgId     所属机构ID
     * @param excludeId 排除的部门ID
     * @return true-存在，false-不存在
     */
    private boolean isDeptNameExistsExcludingId(String deptName, Long orgId, Long excludeId) {
        return count(new LambdaQueryWrapper<DeptEntity>()
                .eq(DeptEntity::getDeptName, deptName)
                .eq(DeptEntity::getOrgId, orgId)
                .ne(DeptEntity::getId, excludeId)) > 0;
    }

    /**
     * 生成部门编码
     * 编码规则：DEPT-序号（3位）
     *
     * @return 部门编码
     */
    private String generateDeptCode() {
        String prefix = "DEPT-";
        // 查询当前前缀下的最大序号
        LambdaQueryWrapper<DeptEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DeptEntity::getDeptCode, prefix)
                .orderByDesc(DeptEntity::getDeptCode)
                .last("LIMIT 1");
        DeptEntity lastDept = getOne(wrapper);
        int maxSeq = 0;
        if (lastDept != null && StringUtils.hasText(lastDept.getDeptCode())) {
            String code = lastDept.getDeptCode();
            String seqStr = code.replace(prefix, "");
            try {
                maxSeq = Integer.parseInt(seqStr);
            } catch (NumberFormatException e) {
                maxSeq = 0;
            }
        }
        // 生成新编码
        return prefix + String.format("%03d", maxSeq + 1);
    }

    /**
     * 校验该部门下是否有用户
     *
     * @param deptId 部门ID
     * @return true-有用户，false-无用户
     */
    private boolean hasUsers(Long deptId) {
        return userMapper.countByDeptId(deptId) > 0;
    }
}
