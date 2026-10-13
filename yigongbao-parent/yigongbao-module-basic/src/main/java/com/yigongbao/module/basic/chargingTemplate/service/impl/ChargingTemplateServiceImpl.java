package com.yigongbao.module.basic.chargingTemplate.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.chargingTemplate.dto.ChargingTemplateItemDTO;
import com.yigongbao.module.basic.chargingTemplate.dto.CreateChargingTemplateDTO;
import com.yigongbao.module.basic.chargingTemplate.dto.UpdateChargingTemplateDTO;
import com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateEntity;
import com.yigongbao.module.basic.chargingTemplate.entity.ChargingTemplateItemEntity;
import com.yigongbao.module.basic.chargingTemplate.mapper.ChargingTemplateItemMapper;
import com.yigongbao.module.basic.chargingTemplate.mapper.ChargingTemplateMapper;
import com.yigongbao.module.basic.chargingTemplate.service.ChargingTemplateService;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateDetailVO;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateItemVO;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateVO;
import com.yigongbao.module.basic.rebuildProject.entity.RebuildProjectEntity;
import com.yigongbao.module.basic.rebuildProject.mapper.RebuildProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收费模板 Service 实现类
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingTemplateServiceImpl extends ServiceImpl<ChargingTemplateMapper, ChargingTemplateEntity>
        implements ChargingTemplateService {

    private final ChargingTemplateItemMapper itemMapper;
    private final RebuildProjectMapper rebuildProjectMapper;

    /**
     * 分页查询收费模板列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param templateName 模板名称（模糊查询）
     * @return 分页结果
     */
    @Override
    public IPage<ChargingTemplateVO> listPage(Integer pageNum, Integer pageSize, String templateName) {
        templateName = StrUtil.isBlank(templateName) ? null : templateName;

        Page<ChargingTemplateEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChargingTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(templateName != null, ChargingTemplateEntity::getTemplateName, templateName)
                .orderByDesc(ChargingTemplateEntity::getCreateTime);

        IPage<ChargingTemplateEntity> entityPage = page(page, wrapper);

        List<Long> templateIds = entityPage.getRecords().stream()
                .map(ChargingTemplateEntity::getId)
                .collect(Collectors.toList());

        Map<Long, Long> templateBodyPartMap = templateIds.isEmpty() ?
                Map.of() : getTemplateBodyPartMap(templateIds);

        IPage<ChargingTemplateVO> voPage = entityPage.convert(entity -> {
            ChargingTemplateVO vo = new ChargingTemplateVO();
            BeanUtil.copyProperties(entity, vo);
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
            vo.setBodyPartId(templateBodyPartMap.get(entity.getId()));
            return vo;
        });

        return voPage;
    }

    /**
     * 根据ID查询模板详情（含差异统计）
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @Override
    public ChargingTemplateDetailVO getDetailById(Long id) {
        ChargingTemplateEntity template = getById(id);
        if (template == null) {
            log.warn("收费模板不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_NOT_FOUND);
        }

        List<ChargingTemplateItemEntity> items = itemMapper.selectList(
                new LambdaQueryWrapper<ChargingTemplateItemEntity>()
                        .eq(ChargingTemplateItemEntity::getTemplateId, id)
        );

        Long totalActiveProjects = rebuildProjectMapper.selectCount(
                new LambdaQueryWrapper<RebuildProjectEntity>()
                        .eq(RebuildProjectEntity::getStatus, StatusConstants.NORMAL)
        );

        Set<Long> itemProjectIds = items.stream()
                .map(ChargingTemplateItemEntity::getRebuildProjectId)
                .collect(Collectors.toSet());

        List<RebuildProjectEntity> itemProjects = itemProjectIds.isEmpty() ?
                List.of() :
                rebuildProjectMapper.selectList(
                        new LambdaQueryWrapper<RebuildProjectEntity>()
                                .select(RebuildProjectEntity::getId, RebuildProjectEntity::getName, RebuildProjectEntity::getStatus)
                                .in(RebuildProjectEntity::getId, itemProjectIds)
                );

        Map<Long, String> projectNameMap = itemProjects.stream()
                .collect(Collectors.toMap(RebuildProjectEntity::getId, RebuildProjectEntity::getName));

        Set<Long> activeItemProjectIds = itemProjects.stream()
                .filter(p -> p.getStatus().equals(StatusConstants.NORMAL))
                .map(RebuildProjectEntity::getId)
                .collect(Collectors.toSet());

        int obsoleteCount = (int) itemProjectIds.stream()
                .filter(projectId -> !activeItemProjectIds.contains(projectId))
                .count();

        int missingCount = totalActiveProjects.intValue() - items.size() + obsoleteCount;

        ChargingTemplateDetailVO vo = new ChargingTemplateDetailVO();
        BeanUtil.copyProperties(template, vo);
        vo.setStatusName(StatusConstants.getStatusName(template.getStatus()));
        vo.setBodyPartId(getTemplateBodyPartId(items));
        vo.setTotalActiveProjects(totalActiveProjects.intValue());
        vo.setMissingCount(missingCount);
        vo.setObsoleteCount(obsoleteCount);
        vo.setItems(items.stream().map(item -> {
            ChargingTemplateItemVO itemVO = new ChargingTemplateItemVO();
            BeanUtil.copyProperties(item, itemVO);
            itemVO.setProjectName(projectNameMap.getOrDefault(item.getRebuildProjectId(), "已删除"));
            itemVO.setIsObsolete(!activeItemProjectIds.contains(item.getRebuildProjectId()));
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 创建收费模板
     *
     * @param dto 创建参数
     * @return 模板ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateChargingTemplateDTO dto) {
        long count = count(new LambdaQueryWrapper<ChargingTemplateEntity>()
                .eq(ChargingTemplateEntity::getTemplateName, dto.getTemplateName()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_NAME_EXISTS);
        }

        validateTemplateItems(dto.getItems());

        ChargingTemplateEntity entity = new ChargingTemplateEntity();
        BeanUtil.copyProperties(dto, entity, "items");
        if (entity.getStatus() == null) {
            entity.setStatus(StatusConstants.NORMAL);
        }
        save(entity);

        List<ChargingTemplateItemEntity> itemEntities = convertToItemEntities(dto.getItems(), entity.getId());
        itemEntities.forEach(itemMapper::insert);

        log.info("创建收费模板: id={}, templateName={}, itemCount={}",
            entity.getId(), entity.getTemplateName(), itemEntities.size());
        return entity.getId();
    }

    /**
     * 更新收费模板
     *
     * @param id 模板ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateChargingTemplateDTO dto) {
        ChargingTemplateEntity existing = getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_NOT_FOUND);
        }

        long count = count(new LambdaQueryWrapper<ChargingTemplateEntity>()
                .eq(ChargingTemplateEntity::getTemplateName, dto.getTemplateName())
                .ne(ChargingTemplateEntity::getId, id));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_NAME_EXISTS);
        }

        validateTemplateItems(dto.getItems());

        BeanUtil.copyProperties(dto, existing, "items");
        updateById(existing);

        itemMapper.delete(new LambdaQueryWrapper<ChargingTemplateItemEntity>()
                .eq(ChargingTemplateItemEntity::getTemplateId, id));

        List<ChargingTemplateItemEntity> itemEntities = convertToItemEntities(dto.getItems(), id);
        itemEntities.forEach(itemMapper::insert);

        log.info("更新收费模板: id={}, templateName={}, itemCount={}",
            id, existing.getTemplateName(), itemEntities.size());
    }

    /**
     * 删除收费模板
     *
     * @param id 模板ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        ChargingTemplateEntity template = getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_NOT_FOUND);
        }

        removeById(id);

        itemMapper.delete(new LambdaQueryWrapper<ChargingTemplateItemEntity>()
                .eq(ChargingTemplateItemEntity::getTemplateId, id));

        log.info("删除收费模板: id={}, templateName={}", id, template.getTemplateName());
    }

    /**
     * 查询收费模板列表（不分页，用于下拉选择）
     *
     * @param templateName 模板名称（模糊查询）
     * @return 模板列表
     */
    @Override
    public List<ChargingTemplateVO> list(String templateName) {
        LambdaQueryWrapper<ChargingTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(templateName), ChargingTemplateEntity::getTemplateName, templateName)
                .eq(ChargingTemplateEntity::getStatus, StatusConstants.NORMAL)
                .orderByDesc(ChargingTemplateEntity::getCreateTime);

        List<ChargingTemplateEntity> entities = list(wrapper);

        return entities.stream().map(entity -> {
            ChargingTemplateVO vo = new ChargingTemplateVO();
            BeanUtil.copyProperties(entity, vo);
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    private void validateTemplateItems(List<ChargingTemplateItemDTO> items) {
        Set<Long> projectIds = items.stream()
                .map(ChargingTemplateItemDTO::getRebuildProjectId)
                .collect(Collectors.toSet());

        if (projectIds.size() < items.size()) {
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_DUPLICATE_PROJECT);
        }

        long existingCount = rebuildProjectMapper.selectCount(
                new LambdaQueryWrapper<RebuildProjectEntity>()
                        .in(RebuildProjectEntity::getId, projectIds)
                        .eq(RebuildProjectEntity::getStatus, StatusConstants.NORMAL)
        );
        if (existingCount != projectIds.size()) {
            throw new BusinessException(ErrorCodeEnum.CHARGING_TEMPLATE_REBUILD_PROJECT_NOT_FOUND);
        }
    }

    private List<ChargingTemplateItemEntity> convertToItemEntities(List<ChargingTemplateItemDTO> dtos, Long templateId) {
        return dtos.stream()
                .map(itemDTO -> {
                    ChargingTemplateItemEntity item = new ChargingTemplateItemEntity();
                    BeanUtil.copyProperties(itemDTO, item);
                    item.setTemplateId(templateId);
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 从明细项中反查部位ID
     * 若所有明细项的重建项目都属于同一部位，返回该部位ID；否则返回null
     */
    private Long getTemplateBodyPartId(List<ChargingTemplateItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        Set<Long> projectIds = items.stream()
                .map(ChargingTemplateItemEntity::getRebuildProjectId)
                .collect(Collectors.toSet());

        List<RebuildProjectEntity> projects = rebuildProjectMapper.selectList(
                new LambdaQueryWrapper<RebuildProjectEntity>()
                        .select(RebuildProjectEntity::getId, RebuildProjectEntity::getBodyPartId)
                        .in(RebuildProjectEntity::getId, projectIds)
        );

        Set<Long> bodyPartIds = projects.stream()
                .map(RebuildProjectEntity::getBodyPartId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        return bodyPartIds.size() == 1 ? bodyPartIds.iterator().next() : null;
    }

    /**
     * 批量查询多个模板的部位ID映射
     */
    private Map<Long, Long> getTemplateBodyPartMap(List<Long> templateIds) {
        List<ChargingTemplateItemEntity> allItems = itemMapper.selectList(
                new LambdaQueryWrapper<ChargingTemplateItemEntity>()
                        .in(ChargingTemplateItemEntity::getTemplateId, templateIds)
        );

        Map<Long, List<ChargingTemplateItemEntity>> itemsByTemplate = allItems.stream()
                .collect(Collectors.groupingBy(ChargingTemplateItemEntity::getTemplateId));

        Map<Long, Long> result = new java.util.HashMap<>();
        for (Long templateId : templateIds) {
            Long bodyPartId = getTemplateBodyPartId(itemsByTemplate.getOrDefault(templateId, List.of()));
            result.put(templateId, bodyPartId);
        }
        return result;
    }
}
