package com.yigongbao.module.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.dict.convert.DictConvert;
import com.yigongbao.module.system.dict.dto.CreateDictDTO;
import com.yigongbao.module.system.dict.dto.UpdateDictDTO;
import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.mapper.DictMapper;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 字典 Service 实现类
 * 处理字典相关的业务逻辑，包括字典类型、字典数据、树形结构等
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictServiceImpl extends ServiceImpl<DictMapper, DictEntity> implements DictService {

    private static final Long ROOT_PARENT_ID = 0L;

    /**
     * 字典类型列表（根节点）
     *
     * @return 字典类型列表
     */
    @Override
    public List<DictVO> listType() {
        log.info("查询字典类型列表");
        try {
            List<DictEntity> list = list(new LambdaQueryWrapper<DictEntity>()
                    .eq(DictEntity::getParentId, ROOT_PARENT_ID)
                    .orderByAsc(DictEntity::getSort)
                    .orderByAsc(DictEntity::getId));
            log.info("查询字典类型列表成功，数量={}", list.size());
            return DictConvert.toVOList(list);
        } catch (Exception e) {
            log.error("查询字典类型列表异常", e);
            throw e;
        }
    }

    /**
     * 根据类型编码获取字典数据列表
     *
     * @param typeCode 类型编码（根节点的dictCode）
     * @return 字典数据列表
     */
    @Override
    public List<DictVO> listByTypeCode(String typeCode) {
        log.info("根据类型编码查询字典数据，typeCode={}", typeCode);
        try {
            // 查询所有数据并筛选根节点
            List<DictEntity> allList = list(new LambdaQueryWrapper<DictEntity>()
                    .orderByAsc(DictEntity::getSort)
                    .orderByAsc(DictEntity::getId));
            // 查找根节点
            DictEntity typeEntity = allList.stream()
                    .filter(e -> Objects.equals(e.getDictCode(), typeCode) && Objects.equals(e.getParentId(), ROOT_PARENT_ID))
                    .findFirst()
                    .orElse(null);
            if (typeEntity == null) {
                log.warn("字典类型不存在，typeCode={}", typeCode);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 筛选子节点
            List<DictEntity> list = allList.stream()
                    .filter(e -> Objects.equals(e.getParentId(), typeEntity.getId()))
                    .sorted(Comparator.comparing(DictEntity::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                    .sorted(Comparator.comparing(DictEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            log.info("查询字典数据成功，数量={}", list.size());
            return DictConvert.toVOList(list);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("根据类型编码查询字典数据异常，typeCode={}", typeCode, e);
            throw e;
        }
    }

    /**
     * 获取完整树形结构
     *
     * @return 树形结构列表
     */
    @Override
    public List<DictVO> listTree() {
        log.info("查询字典完整树形结构");
        try {
            List<DictEntity> allList = list(new LambdaQueryWrapper<DictEntity>()
                    .orderByAsc(DictEntity::getSort)
                    .orderByAsc(DictEntity::getId));
            List<DictVO> tree = buildTree(allList, ROOT_PARENT_ID);
            log.info("查询字典树形结构成功，根节点数量={}", tree.size());
            return tree;
        } catch (Exception e) {
            log.error("查询字典树形结构异常", e);
            throw e;
        }
    }

    /**
     * 获取指定类型的树形结构
     *
     * @param typeCode 类型编码
     * @return 树形结构列表
     */
    @Override
    public List<DictVO> listTreeByTypeCode(String typeCode) {
        log.info("查询字典树形结构，typeCode={}", typeCode);
        try {
            // 查询所有数据并筛选根节点
            List<DictEntity> allList = list(new LambdaQueryWrapper<DictEntity>()
                    .orderByAsc(DictEntity::getSort)
                    .orderByAsc(DictEntity::getId));
            // 查找根节点
            DictEntity typeEntity = allList.stream()
                    .filter(e -> Objects.equals(e.getDictCode(), typeCode) && Objects.equals(e.getParentId(), ROOT_PARENT_ID))
                    .findFirst()
                    .orElse(null);
            if (typeEntity == null) {
                log.warn("字典类型不存在，typeCode={}", typeCode);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 构建树形结构，根节点作为树的根
            DictVO rootVO = DictConvert.toVO(typeEntity);
            rootVO.setChildren(buildTree(allList, typeEntity.getId()));
            if (rootVO.getChildren() != null && rootVO.getChildren().isEmpty()) {
                rootVO.setChildren(null);
            }
            log.info("查询字典树形结构成功");
            return Arrays.asList(rootVO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询字典树形结构异常，typeCode={}", typeCode, e);
            throw e;
        }
    }

    /**
     * 获取下拉选项（叶子节点）
     *
     * @param typeCode 类型编码
     * @return 叶子节点列表
     */
    @Override
    public List<DictVO> listOptions(String typeCode) {
        log.info("查询字典下拉选项，typeCode={}", typeCode);
        try {
            // 查询所有数据并筛选根节点
            List<DictEntity> allList = list(new LambdaQueryWrapper<DictEntity>()
                    .orderByAsc(DictEntity::getSort)
                    .orderByAsc(DictEntity::getId));
            // 查找根节点
            DictEntity typeEntity = allList.stream()
                    .filter(e -> Objects.equals(e.getDictCode(), typeCode) && Objects.equals(e.getParentId(), ROOT_PARENT_ID))
                    .findFirst()
                    .orElse(null);
            if (typeEntity == null) {
                log.warn("字典类型不存在，typeCode={}", typeCode);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            List<DictVO> result = new ArrayList<>();
            collectLeafNodes(allList, typeEntity.getId(), result);
            log.info("查询字典下拉选项成功，数量={}", result.size());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询字典下拉选项异常，typeCode={}", typeCode, e);
            throw e;
        }
    }

    /**
     * 根据ID查询字典
     *
     * @param id 字典ID
     * @return 字典VO
     */
    @Override
    public DictVO getById(Long id) {
        log.info("根据ID查询字典，id={}", id);
        try {
            DictEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("字典不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            log.info("查询字典成功，id={}", id);
            return DictConvert.toVO(entity);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("根据ID查询字典异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建字典
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateDictDTO dto) {
        log.info("创建字典，parentId={}, dictName={}", dto.getParentId(), dto.getDictName());
        try {
            // 校验字典编码唯一性
            String newDictCode = generateDictCode(dto.getParentId());
            if (isDictCodeExists(newDictCode, null)) {
                log.warn("字典编码已存在，dictCode={}", newDictCode);
                throw new BusinessException(ErrorCodeEnum.DICT_CODE_EXISTS);
            }
            // 校验字典名称在同一父节点下唯一性
            if (isDictNameExists(dto.getDictName(), dto.getParentId(), null)) {
                log.warn("字典名称在同一父节点下已存在，dictName={}, parentId={}", dto.getDictName(), dto.getParentId());
                throw new BusinessException(ErrorCodeEnum.DICT_NAME_EXISTS);
            }
            // 计算层级
            int level = calculateLevel(dto.getParentId());
            // 构建实体
            DictEntity entity = new DictEntity();
            entity.setParentId(dto.getParentId());
            entity.setDictCode(newDictCode);
            entity.setDictName(dto.getDictName());
            entity.setDictValue(dto.getDictValue());
            entity.setLevel(level);
            entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
            entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            entity.setRemark(dto.getRemark());
            // 保存
            save(entity);
            log.info("创建字典成功，id={}, dictCode={}", entity.getId(), newDictCode);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建字典异常，parentId={}", dto.getParentId(), e);
            throw e;
        }
    }

    /**
     * 更新字典
     *
     * @param id 字典ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateDictDTO dto) {
        log.info("更新字典，id={}", id);
        try {
            DictEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("字典不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 校验字典名称在同一父节点下唯一性（排除自身）
            if (isDictNameExists(dto.getDictName(), entity.getParentId(), id)) {
                log.warn("字典名称在同一父节点下已存在，dictName={}, parentId={}", dto.getDictName(), entity.getParentId());
                throw new BusinessException(ErrorCodeEnum.DICT_NAME_EXISTS);
            }
            // 更新非核心字段
            entity.setDictName(dto.getDictName());
            entity.setDictValue(dto.getDictValue());
            if (dto.getSort() != null) {
                entity.setSort(dto.getSort());
            }
            if (dto.getStatus() != null) {
                entity.setStatus(dto.getStatus());
            }
            entity.setRemark(dto.getRemark());
            // 保存
            updateById(entity);
            log.info("更新字典成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新字典异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除字典
     *
     * @param id 字典ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        log.info("删除字典，id={}", id);
        try {
            DictEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("字典不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 检查是否有子节点
            if (hasChildren(id)) {
                log.warn("字典存在子节点，无法删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_HAS_CHILDREN);
            }
            // 逻辑删除
            super.removeById(id);
            log.info("删除字典成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除字典异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改字典状态（级联）
     *
     * @param id 字典ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改字典状态，id={}, status={}", id, status);
        try {
            // 校验状态值合法性
            if (status == null || (status != StatusConstants.DISABLED && status != StatusConstants.NORMAL)) {
                log.warn("状态值不合法，status={}", status);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            DictEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("字典不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            // 递归获取所有子节点ID
            List<Long> allIds = getAllChildrenIds(id);
            allIds.add(id);
            // 逐条更新，确保自动填充生效（updateTime、updateBy）
            for (Long dictId : allIds) {
                DictEntity updateEntity = new DictEntity();
                updateEntity.setId(dictId);
                updateEntity.setStatus(status);
                super.updateById(updateEntity);
            }
            log.info("修改字典状态成功，id={}, status={}, 级联数量={}", id, status, allIds.size());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改字典状态异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 生成子节点编码
     * 规则：
     * - 父节点为根节点(parentId=0)：生成 1, 2, 3...
     * - 父节点为子节点：生成 1.1, 1.2, 1.3...
     * - 二级子节点：生成 1.1.1, 1.1.2...
     *
     * @param parentId 父节点ID
     * @return 新编码
     */
    private String generateDictCode(Long parentId) {
        if (ROOT_PARENT_ID.equals(parentId)) {
            // 根节点：查询最大根编码 + 1
            return String.valueOf(getMaxRootCode() + 1);
        } else {
            // 子节点：查询父节点编码 + "." + 查询最大子编码的最后一个分段 + 1
            DictEntity parent = super.getById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            String parentCode = parent.getDictCode();
            String maxChildCode = getMaxChildCode(parentId);
            if (maxChildCode == null) {
                return parentCode + ".1";
            }
            // 解析最大子编码的最后一个分段
            String[] parts = maxChildCode.split("\\.");
            int lastPart = Integer.parseInt(parts[parts.length - 1]);
            return parentCode + "." + (lastPart + 1);
        }
    }

    /**
     * 判断字典编码是否存在
     *
     * @param dictCode 字典编码
     * @param excludeId 排除的ID（更新时使用）
     * @return true-存在，false-不存在
     */
    private boolean isDictCodeExists(String dictCode, Long excludeId) {
        LambdaQueryWrapper<DictEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictEntity::getDictCode, dictCode);
        if (excludeId != null) {
            wrapper.ne(DictEntity::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    /**
     * 判断字典名称在同一父节点下是否存在
     *
     * @param dictName 字典名称
     * @param parentId 父节点ID
     * @param excludeId 排除的ID（更新时使用）
     * @return true-存在，false-不存在
     */
    private boolean isDictNameExists(String dictName, Long parentId, Long excludeId) {
        LambdaQueryWrapper<DictEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictEntity::getDictName, dictName)
                .eq(DictEntity::getParentId, parentId);
        if (excludeId != null) {
            wrapper.ne(DictEntity::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    /**
     * 获取最大根编码
     *
     * @return 最大根编码的数值
     */
    private int getMaxRootCode() {
        List<DictEntity> list = list(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getParentId, ROOT_PARENT_ID)
                .orderByDesc(DictEntity::getDictCode)
                .last("LIMIT 1"));
        if (list.isEmpty()) {
            return 0;
        }
        String maxCode = list.get(0).getDictCode();
        return Integer.parseInt(maxCode);
    }

    /**
     * 获取父节点下最大的子编码
     *
     * @param parentId 父节点ID
     * @return 最大子编码，不存在返回 null
     */
    private String getMaxChildCode(Long parentId) {
        List<DictEntity> list = list(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getParentId, parentId)
                .orderByDesc(DictEntity::getDictCode)
                .last("LIMIT 1"));
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0).getDictCode();
    }

    /**
     * 计算层级
     *
     * @param parentId 父节点ID
     * @return 层级
     */
    private int calculateLevel(Long parentId) {
        if (ROOT_PARENT_ID.equals(parentId)) {
            return 1;
        }
        DictEntity parent = super.getById(parentId);
        if (parent == null) {
            return 1;
        }
        return parent.getLevel() + 1;
    }

    /**
     * 递归构建树形结构
     *
     * @param allList 所有字典数据
     * @param parentId 父节点ID
     * @return 树形列表
     */
    private List<DictVO> buildTree(List<DictEntity> allList, Long parentId) {
        return allList.stream()
                .filter(e -> Objects.equals(e.getParentId(), parentId))
                .map(e -> {
                    DictVO vo = DictConvert.toVO(e);
                    vo.setChildren(buildTree(allList, e.getId()));
                    // 叶子节点不返回 children 或返回空列表
                    if (vo.getChildren() != null && vo.getChildren().isEmpty()) {
                        vo.setChildren(null);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 递归收集叶子节点
     *
     * @param allList 所有字典数据
     * @param parentId 父节点ID
     * @param result 结果列表
     */
    private void collectLeafNodes(List<DictEntity> allList, Long parentId, List<DictVO> result) {
        for (DictEntity entity : allList) {
            if (Objects.equals(entity.getParentId(), parentId)) {
                // 检查是否有子节点
                boolean hasChild = allList.stream()
                        .anyMatch(e -> Objects.equals(e.getParentId(), entity.getId()));
                if (!hasChild) {
                    // 叶子节点
                    result.add(DictConvert.toVO(entity));
                } else {
                    // 非叶子节点，递归处理
                    collectLeafNodes(allList, entity.getId(), result);
                }
            }
        }
    }

    /**
     * 判断是否有子节点
     *
     * @param id 字典ID
     * @return true-有子节点，false-无子节点
     */
    private boolean hasChildren(Long id) {
        return count(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getParentId, id)) > 0;
    }

    /**
     * 递归获取所有子节点ID
     *
     * @param parentId 父节点ID
     * @return 所有子节点ID列表
     */
    private List<Long> getAllChildrenIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        List<DictEntity> children = list(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getParentId, parentId));
        for (DictEntity child : children) {
            result.add(child.getId());
            result.addAll(getAllChildrenIds(child.getId()));
        }
        return result;
    }
}
