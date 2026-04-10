package com.yigongbao.module.basic.bodyPart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.bodyPart.convert.BodyPartConvert;
import com.yigongbao.module.basic.bodyPart.dto.CreateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.dto.UpdateBodyPartDTO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.bodyPart.entity.BodyPartEntity;
import com.yigongbao.module.basic.bodyPart.mapper.BodyPartMapper;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartOptionVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 重建部位 Service 实现类
 * 处理部位相关的业务逻辑，包括CRUD、树形结构管理等
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BodyPartServiceImpl extends ServiceImpl<BodyPartMapper, BodyPartEntity> implements BodyPartService {

    private final CodeGeneratorService codeGeneratorService;

    /**
     * 获取部位树形结构
     *
     * @return 部位树形列表
     */
    @Override
    public List<BodyPartVO> listTree() {
        log.info("获取部位树形结构");
        try {
            List<BodyPartEntity> allList = list(new LambdaQueryWrapper<BodyPartEntity>()
                    .orderByAsc(BodyPartEntity::getSort)
                    .orderByDesc(BodyPartEntity::getCreateTime));
            List<BodyPartVO> voList = allList.stream().map(this::toVO).collect(Collectors.toList());
            List<BodyPartVO> tree = buildTree(voList);
            log.info("获取部位树形结构成功，数量={}", tree.size());
            return tree;
        } catch (Exception e) {
            log.error("获取部位树形结构异常", e);
            throw e;
        }
    }

    /**
     * 获取部位下拉选项（仅返回启用状态）
     *
     * @return 部位下拉选项列表
     */
    @Override
    public List<BodyPartOptionVO> listOptions() {
        log.info("获取部位下拉选项");
        try {
            List<BodyPartEntity> allList = list(new LambdaQueryWrapper<BodyPartEntity>()
                    .eq(BodyPartEntity::getStatus, StatusConstants.NORMAL)
                    .orderByAsc(BodyPartEntity::getSort));
            List<BodyPartOptionVO> voList = allList.stream().map(this::toOptionVO).collect(Collectors.toList());
            List<BodyPartOptionVO> tree = buildOptionTree(voList);
            log.info("获取部位下拉选项成功，数量={}", tree.size());
            return tree;
        } catch (Exception e) {
            log.error("获取部位下拉选项异常", e);
            throw e;
        }
    }

    /**
     * 查询部位详情
     *
     * @param id 部位ID
     * @return 部位详情
     */
    @Override
    public BodyPartDetailVO getDetailById(Long id) {
        log.info("查询部位详情，id={}", id);
        try {
            BodyPartEntity entity = getById(id);
            if (entity == null) {
                log.warn("部位不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
            }
            BodyPartDetailVO vo = toDetailVO(entity);
            if (entity.getParentId() != null && entity.getParentId() > 0) {
                BodyPartEntity parent = getById(entity.getParentId());
                if (parent != null) {
                    vo.setParentName(parent.getName());
                }
            }
            log.info("查询部位详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询部位详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建部位
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBodyPart(CreateBodyPartDTO dto) {
        log.info("创建部位，name={}", dto.getName());
        try {
            if (isNameExistsInParent(dto.getName(), dto.getParentId())) {
                log.warn("部位名称已存在，name={}, parentId={}", dto.getName(), dto.getParentId());
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NAME_EXISTS);
            }
            BodyPartEntity entity = BodyPartConvert.toEntity(dto);
            entity.setCode(codeGeneratorService.generate(CodeRuleConstants.BODYPART_NO));
            entity.setLevel(dto.getParentId() == 0 ? 1 : 2);
            entity.setStatus(Objects.requireNonNullElse(dto.getStatus(), StatusConstants.NORMAL));
            entity.setSort(Objects.requireNonNullElse(dto.getSort(), 0));
            save(entity);
            log.info("创建部位成功，id={}", entity.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建部位异常，name={}", dto.getName(), e);
            throw e;
        }
    }

    /**
     * 更新部位
     *
     * @param id  部位ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBodyPart(Long id, UpdateBodyPartDTO dto) {
        log.info("更新部位，id={}", id);
        try {
            BodyPartEntity entity = getById(id);
            if (entity == null) {
                log.warn("部位不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
            }
            if (isNameExistsInParentExcludingId(dto.getName(), entity.getParentId(), id)) {
                log.warn("部位名称已存在，name={}", dto.getName());
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NAME_EXISTS);
            }
            entity.setName(dto.getName());
            entity.setSort(Objects.requireNonNullElse(dto.getSort(), 0));
            entity.setStatus(Objects.requireNonNullElse(dto.getStatus(), entity.getStatus()));
            entity.setRemark(dto.getRemark());
            updateById(entity);
            log.info("更新部位成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新部位异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除部位
     *
     * @param id 部位ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBodyPart(Long id) {
        log.info("删除部位，id={}", id);
        try {
            BodyPartEntity entity = getById(id);
            if (entity == null) {
                log.warn("部位不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
            }
            if (hasChildren(id)) {
                log.warn("该部位存在子部位，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_HAS_CHILDREN);
            }
            removeById(id);
            log.info("删除部位成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除部位异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改部位状态
     *
     * @param id     部位ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改部位状态，id={}, status={}", id, status);
        try {
            if (status == null || (status != StatusConstants.DISABLED && status != StatusConstants.NORMAL)) {
                log.warn("状态值不合法，status={}", status);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            BodyPartEntity entity = getById(id);
            if (entity == null) {
                log.warn("部位不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改部位状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改部位状态异常，id={}", id, e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    private BodyPartVO toVO(BodyPartEntity entity) {
        if (entity == null) {
            return null;
        }
        BodyPartVO vo = BodyPartConvert.toVO(entity);
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        return vo;
    }

    private BodyPartOptionVO toOptionVO(BodyPartEntity entity) {
        if (entity == null) {
            return null;
        }
        BodyPartOptionVO vo = new BodyPartOptionVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setLevel(entity.getLevel());
        return vo;
    }

    private BodyPartDetailVO toDetailVO(BodyPartEntity entity) {
        if (entity == null) {
            return null;
        }
        BodyPartDetailVO vo = new BodyPartDetailVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setLevel(entity.getLevel());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private List<BodyPartVO> buildTree(List<BodyPartVO> list) {
        Map<Long, List<BodyPartVO>> childrenMap = list.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() > 0)
                .collect(Collectors.groupingBy(BodyPartVO::getParentId));
        list.forEach(item -> item.setChildren(childrenMap.getOrDefault(item.getId(), new ArrayList<>())));
        return list.stream()
                .filter(item -> item.getParentId() == null || item.getParentId() == 0)
                .collect(Collectors.toList());
    }

    private List<BodyPartOptionVO> buildOptionTree(List<BodyPartOptionVO> list) {
        Map<Long, List<BodyPartOptionVO>> childrenMap = list.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() > 0)
                .collect(Collectors.groupingBy(BodyPartOptionVO::getParentId));
        list.forEach(item -> item.setChildren(childrenMap.getOrDefault(item.getId(), new ArrayList<>())));
        return list.stream()
                .filter(item -> item.getParentId() == null || item.getParentId() == 0)
                .collect(Collectors.toList());
    }

    private boolean hasChildren(Long parentId) {
        return count(new LambdaQueryWrapper<BodyPartEntity>()
                .eq(BodyPartEntity::getParentId, parentId)) > 0;
    }

    private boolean isNameExistsInParent(String name, Long parentId) {
        return count(new LambdaQueryWrapper<BodyPartEntity>()
                .eq(BodyPartEntity::getName, name)
                .eq(BodyPartEntity::getParentId, parentId)) > 0;
    }

    private boolean isNameExistsInParentExcludingId(String name, Long parentId, Long excludeId) {
        return count(new LambdaQueryWrapper<BodyPartEntity>()
                .eq(BodyPartEntity::getName, name)
                .eq(BodyPartEntity::getParentId, parentId)
                .ne(BodyPartEntity::getId, excludeId)) > 0;
    }
}
