package com.yigongbao.module.basic.rebuildProject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.bodyPart.entity.BodyPartEntity;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.rebuildProject.convert.RebuildProjectConvert;
import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.dto.UpdateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.entity.RebuildProjectEntity;
import com.yigongbao.module.basic.rebuildProject.mapper.RebuildProjectMapper;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.basic.rebuildProject.vo.ProjectOptionItemVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectOptionVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 重建项目 Service 实现类
 * 处理项目相关的业务逻辑，包括CRUD、树形结构管理等
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RebuildProjectServiceImpl extends ServiceImpl<RebuildProjectMapper, RebuildProjectEntity>
        implements RebuildProjectService {

    private final BodyPartService bodyPartService;
    private final CodeGeneratorService codeGeneratorService;

    /** 部位名称缓存（ConcurrentHashMap 线程安全，本地缓存避免重复查询） */
    private final Map<Long, String> bodyPartNameCache = new ConcurrentHashMap<>();

    /** 重建项目缓存（用于 getDetailById 中快速获取父项目名称） */
    private final Map<Long, String> projectNameCache = new ConcurrentHashMap<>();

    /**
     * 获取项目树形结构（按部位分组）
     *
     * @param categoryCode 项目分类编码（可选，传入则精确匹配，不传则返回全部）
     * @return 项目树形列表
     */
    @Override
    public List<RebuildProjectVO> listTree(String categoryCode) {
        log.info("获取项目树形结构，categoryCode={}", categoryCode);
        try {
            Map<Long, String> bodyPartNameMap = getBodyPartNameMap();
            LambdaQueryWrapper<RebuildProjectEntity> wrapper = buildQueryWrapper(null, categoryCode);
            List<RebuildProjectEntity> allList = list(wrapper
                    .orderByAsc(RebuildProjectEntity::getSort)
                    .orderByDesc(RebuildProjectEntity::getCreateTime));
            List<RebuildProjectVO> voList = allList.stream()
                    .map(e -> toVO(e, bodyPartNameMap))
                    .collect(Collectors.toList());
            List<RebuildProjectVO> tree = buildTree(voList);
            log.info("获取项目树形结构成功");
            return tree;
        } catch (Exception e) {
            log.error("获取项目树形结构异常", e);
            throw e;
        }
    }

    /**
     * 根据部位ID获取项目列表
     *
     * @param bodyPartId 部位ID
     * @param category   项目分类（可选，不传则返回全部）
     * @return 该部位下的项目树
     */
    @Override
    public List<RebuildProjectVO> listByBodyPartId(Long bodyPartId, String categoryCode) {
        log.info("根据部位ID获取项目列表，bodyPartId={}, categoryCode={}", bodyPartId, categoryCode);
        try {
            BodyPartEntity bodyPart = bodyPartService.getById(bodyPartId);
            if (bodyPart == null) {
                log.warn("部位不存在，bodyPartId={}", bodyPartId);
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
            }
            Map<Long, String> bodyPartNameMap = getBodyPartNameMap();
            LambdaQueryWrapper<RebuildProjectEntity> wrapper = buildQueryWrapper(bodyPartId, categoryCode);
            List<RebuildProjectEntity> allList = list(wrapper
                    .orderByAsc(RebuildProjectEntity::getSort));
            List<RebuildProjectVO> voList = allList.stream()
                    .map(e -> toVO(e, bodyPartNameMap))
                    .collect(Collectors.toList());
            List<RebuildProjectVO> tree = buildTree(voList);
            log.info("根据部位ID获取项目列表成功，数量={}", tree.size());
            return tree;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("根据部位ID获取项目列表异常，bodyPartId={}", bodyPartId, e);
            throw e;
        }
    }

    /**
     * 获取项目下拉选项
     *
     * @param bodyPartId 部位ID（可选，不传则返回全部）
     * @param category   项目分类（可选，不传则返回全部）
     * @return 项目下拉选项列表
     */
    @Override
    public List<RebuildProjectOptionVO> listOptions(Long bodyPartId, String categoryCode) {
        log.info("获取项目下拉选项，bodyPartId={}, categoryCode={}", bodyPartId, categoryCode);
        try {
            Map<Long, String> bodyPartNameMap = getBodyPartNameMap();
            LambdaQueryWrapper<RebuildProjectEntity> wrapper = buildQueryWrapper(bodyPartId, categoryCode);
            wrapper.eq(RebuildProjectEntity::getStatus, StatusConstants.NORMAL)
                    .orderByAsc(RebuildProjectEntity::getBodyPartId)
                    .orderByAsc(RebuildProjectEntity::getSort);
            List<RebuildProjectEntity> allList = list(wrapper);
            List<RebuildProjectVO> voList = allList.stream()
                    .map(e -> toVO(e, bodyPartNameMap))
                    .collect(Collectors.toList());
            List<RebuildProjectOptionVO> result = buildOptionTree(voList, bodyPartNameMap);
            log.info("获取项目下拉选项成功");
            return result;
        } catch (Exception e) {
            log.error("获取项目下拉选项异常", e);
            throw e;
        }
    }

    /**
     * 查询项目详情
     *
     * @param id 项目ID
     * @return 项目详情
     */
    @Override
    public RebuildProjectDetailVO getDetailById(Long id) {
        log.info("查询项目详情，id={}", id);
        try {
            RebuildProjectEntity entity = getById(id);
            if (entity == null) {
                log.warn("项目不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND);
            }
            RebuildProjectDetailVO vo = toDetailVO(entity);
            // 填充部位名称（使用缓存）
            if (entity.getBodyPartId() != null) {
                vo.setBodyPartName(getBodyPartNameMap().get(entity.getBodyPartId()));
            }
            // 填充父项目名称（使用缓存，消除 N+1）
            if (entity.getParentId() != null && entity.getParentId() > 0) {
                vo.setParentName(getProjectNameCache().get(entity.getParentId()));
            }
            log.info("查询项目详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询项目详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建项目
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProject(CreateRebuildProjectDTO dto) {
        log.info("创建项目，name={}", dto.getName());
        try {
            BodyPartEntity bodyPart = bodyPartService.getById(dto.getBodyPartId());
            if (bodyPart == null) {
                log.warn("部位不存在，bodyPartId={}", dto.getBodyPartId());
                throw new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND);
            }
            if (isNameExistsInParent(dto.getName(), dto.getParentId())) {
                log.warn("项目名称已存在，name={}", dto.getName());
                throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NAME_EXISTS);
            }
            RebuildProjectEntity entity = RebuildProjectConvert.toEntity(dto);
            entity.setCode(codeGeneratorService.generate(CodeRuleConstants.PROJECT_NO));
            entity.setLevel(dto.getParentId() == 0 ? 1 : 2);
            entity.setStatus(Objects.requireNonNullElse(dto.getStatus(), StatusConstants.NORMAL));
            entity.setSort(Objects.requireNonNullElse(dto.getSort(), 0));
            entity.setCategoryCode(dto.getCategoryCode());
            entity.setCategoryName(resolveCategoryName(dto.getCategoryCode()));
            save(entity);
            log.info("创建项目成功，id={}", entity.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建项目异常，name={}", dto.getName(), e);
            throw e;
        }
    }

    /**
     * 更新项目
     *
     * @param id  项目ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(Long id, UpdateRebuildProjectDTO dto) {
        log.info("更新项目，id={}", id);
        try {
            RebuildProjectEntity entity = getById(id);
            if (entity == null) {
                log.warn("项目不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND);
            }
            if (isNameExistsInParentExcludingId(dto.getName(), dto.getParentId(), id)) {
                log.warn("项目名称已存在，name={}", dto.getName());
                throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NAME_EXISTS);
            }
            entity.setName(dto.getName());
            entity.setBodyPartId(dto.getBodyPartId());
            entity.setParentId(dto.getParentId());
            entity.setStandardPrice(dto.getStandardPrice());
            entity.setUrgentPrice(dto.getUrgentPrice());
            entity.setEstimatedHours(dto.getEstimatedHours());
            entity.setDescription(dto.getDescription());
            entity.setFormingRequirements(dto.getFormingRequirements());
            entity.setSort(Objects.requireNonNullElse(dto.getSort(), 0));
            entity.setStatus(Objects.requireNonNullElse(dto.getStatus(), entity.getStatus()));
            entity.setRemark(dto.getRemark());
            entity.setCategoryCode(dto.getCategoryCode());
            entity.setCategoryName(resolveCategoryName(dto.getCategoryCode()));
            updateById(entity);
            log.info("更新项目成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新项目异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除项目
     *
     * @param id 项目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeProject(Long id) {
        log.info("删除项目，id={}", id);
        try {
            RebuildProjectEntity entity = getById(id);
            if (entity == null) {
                log.warn("项目不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND);
            }
            if (hasChildren(id)) {
                log.warn("该项目存在子项目，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_HAS_CHILDREN);
            }
            removeById(id);
            log.info("删除项目成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除项目异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改项目状态
     *
     * @param id     项目ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改项目状态，id={}, status={}", id, status);
        try {
            if (status == null || (status != StatusConstants.DISABLED && status != StatusConstants.NORMAL)) {
                log.warn("状态值不合法，status={}", status);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            RebuildProjectEntity entity = getById(id);
            if (entity == null) {
                log.warn("项目不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改项目状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改项目状态异常，id={}", id, e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 构建查询条件
     *
     * @param bodyPartId 部位ID（可选）
     * @param category   项目分类（可选）
     * @return 查询条件
     */
    private LambdaQueryWrapper<RebuildProjectEntity> buildQueryWrapper(Long bodyPartId, String categoryCode) {
        LambdaQueryWrapper<RebuildProjectEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Objects.nonNull(bodyPartId), RebuildProjectEntity::getBodyPartId, bodyPartId)
                .eq(StrUtil.isNotBlank(categoryCode), RebuildProjectEntity::getCategoryCode, categoryCode);
        return wrapper;
    }

    private RebuildProjectVO toVO(RebuildProjectEntity entity, Map<Long, String> bodyPartNameMap) {
        if (entity == null) {
            return null;
        }
        RebuildProjectVO vo = RebuildProjectConvert.toVO(entity);
        if (bodyPartNameMap != null && entity.getBodyPartId() != null) {
            vo.setBodyPartName(bodyPartNameMap.get(entity.getBodyPartId()));
        }
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        return vo;
    }

    private RebuildProjectDetailVO toDetailVO(RebuildProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        RebuildProjectDetailVO vo = new RebuildProjectDetailVO();
        vo.setId(entity.getId());
        vo.setBodyPartId(entity.getBodyPartId());
        vo.setParentId(entity.getParentId());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setLevel(entity.getLevel());
        vo.setStandardPrice(entity.getStandardPrice());
        vo.setUrgentPrice(entity.getUrgentPrice());
        vo.setEstimatedHours(entity.getEstimatedHours());
        vo.setDescription(entity.getDescription());
        vo.setFormingRequirements(entity.getFormingRequirements());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        vo.setRemark(entity.getRemark());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 获取部位名称 Map（带本地缓存）
     *
     * @return 部位 ID -> 部位名称 Map
     */
    private Map<Long, String> getBodyPartNameMap() {
        if (bodyPartNameCache.isEmpty()) {
            bodyPartService.list().forEach(bp -> bodyPartNameCache.put(bp.getId(), bp.getName()));
        }
        return bodyPartNameCache;
    }

    /**
     * 获取项目名称 Map（带本地缓存）
     *
     * @return 项目 ID -> 项目名称 Map
     */
    protected Map<Long, String> getProjectNameCache() {
        if (projectNameCache.isEmpty()) {
            list().forEach(p -> projectNameCache.put(p.getId(), p.getName()));
        }
        return projectNameCache;
    }

    private List<RebuildProjectVO> buildTree(List<RebuildProjectVO> list) {
        Map<Long, List<RebuildProjectVO>> childrenMap = list.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() > 0)
                .collect(Collectors.groupingBy(RebuildProjectVO::getParentId));
        list.forEach(item -> item.setChildren(childrenMap.getOrDefault(item.getId(), new ArrayList<>())));
        return list.stream()
                .filter(item -> item.getParentId() == null || item.getParentId() == 0)
                .collect(Collectors.toList());
    }

    private List<RebuildProjectOptionVO> buildOptionTree(List<RebuildProjectVO> voList, Map<Long, String> bodyPartNameMap) {
        Map<Long, List<RebuildProjectVO>> byPart = voList.stream()
                .collect(Collectors.groupingBy(RebuildProjectVO::getBodyPartId));
        List<RebuildProjectOptionVO> result = new ArrayList<>();
        byPart.forEach((partId, projects) -> {
            RebuildProjectOptionVO partVo = new RebuildProjectOptionVO();
            partVo.setBodyPartId(partId);
            partVo.setBodyPartName(bodyPartNameMap.getOrDefault(partId, "未知部位"));
            List<ProjectOptionItemVO> items = projects.stream()
                    .map(this::toOptionItem)
                    .collect(Collectors.toList());
            Map<Long, List<ProjectOptionItemVO>> childMap = items.stream()
                    .filter(i -> i.getParentId() != null && i.getParentId() > 0)
                    .collect(Collectors.groupingBy(ProjectOptionItemVO::getParentId));
            items.forEach(item -> item.setChildren(childMap.getOrDefault(item.getId(), new ArrayList<>())));
            partVo.setChildren(items.stream()
                    .filter(i -> i.getParentId() == null || i.getParentId() == 0)
                    .collect(Collectors.toList()));
            result.add(partVo);
        });
        return result;
    }

    private ProjectOptionItemVO toOptionItem(RebuildProjectVO vo) {
        ProjectOptionItemVO item = new ProjectOptionItemVO();
        item.setId(vo.getId());
        item.setParentId(vo.getParentId());
        item.setName(vo.getName());
        item.setLevel(vo.getLevel());
        return item;
    }

    private boolean hasChildren(Long parentId) {
        return count(new LambdaQueryWrapper<RebuildProjectEntity>()
                .eq(RebuildProjectEntity::getParentId, parentId)) > 0;
    }

    private boolean isNameExistsInParent(String name, Long parentId) {
        return count(new LambdaQueryWrapper<RebuildProjectEntity>()
                .eq(RebuildProjectEntity::getName, name)
                .eq(RebuildProjectEntity::getParentId, parentId)) > 0;
    }

    private boolean isNameExistsInParentExcludingId(String name, Long parentId, Long excludeId) {
        return count(new LambdaQueryWrapper<RebuildProjectEntity>()
                .eq(RebuildProjectEntity::getName, name)
                .eq(RebuildProjectEntity::getParentId, parentId)
                .ne(RebuildProjectEntity::getId, excludeId)) > 0;
    }

    /**
     * 根据分类编码解析分类名称（硬编码映射，避免查库）
     *
     * @param categoryCode 分类编码（字典 dict_code=13）
     * @return 分类名称，未匹配则返回 null
     */
    private String resolveCategoryName(String categoryCode) {
        if (StrUtil.isBlank(categoryCode)) {
            return null;
        }
        return switch (categoryCode) {
            case DictCodeConstants.PROJECT_CATEGORY_MODEL   -> "模型";
            case DictCodeConstants.PROJECT_CATEGORY_GUIDE   -> "导板";
            case DictCodeConstants.PROJECT_CATEGORY_IMPLANT -> "假体";
            case DictCodeConstants.PROJECT_CATEGORY_OTHER   -> "其他";
            default -> null;
        };
    }
}
