package com.yigongbao.module.system.hospitalGroupTemplate.service.impl;

/**
 * 医院组合模板 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-19
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.system.hospitalGroupTemplate.convert.HospitalGroupTemplateConvert;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.HospitalGroupTemplatePageDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.entity.HospitalGroupTemplateDetailEntity;
import com.yigongbao.module.system.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import com.yigongbao.module.system.hospitalGroupTemplate.mapper.HospitalGroupTemplateDetailMapper;
import com.yigongbao.module.system.hospitalGroupTemplate.mapper.HospitalGroupTemplateMapper;
import com.yigongbao.module.system.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateDetailVO;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.user.service.UserHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalGroupTemplateServiceImpl extends ServiceImpl<HospitalGroupTemplateMapper, HospitalGroupTemplateEntity>
        implements HospitalGroupTemplateService {

    private final HospitalGroupTemplateDetailMapper detailMapper;
    private final OrgService orgService;
    private final UserHospitalService userHospitalService;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * 分页查询医院组合模板列表
     *
     * @param dto 分页查询条件，支持模板名称模糊搜索和状态过滤
     * @return 分页结果，每条记录包含模板基本信息及关联医院数量
     */
    @Override
    public IPage<HospitalGroupTemplateVO> listTemplate(HospitalGroupTemplatePageDTO dto) {
        log.info("分页查询医院组合模板列表，dto={}", dto);
        try {
            int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
            int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
            Page<HospitalGroupTemplateEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<HospitalGroupTemplateEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(dto.getTemplateName()), HospitalGroupTemplateEntity::getTemplateName, dto.getTemplateName())
                    .eq(Objects.nonNull(dto.getStatus()), HospitalGroupTemplateEntity::getStatus, dto.getStatus())
                    .orderByDesc(HospitalGroupTemplateEntity::getCreateTime);
            IPage<HospitalGroupTemplateEntity> pageResult = page(page, wrapper);
            IPage<HospitalGroupTemplateVO> voPage = pageResult.convert(this::toVOWithCount);
            log.info("分页查询医院组合模板列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询医院组合模板列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询医院组合模板详情（含明细列表）
     *
     * @param id     模板ID
     * @param userId 用户ID（可选）：传入时 assigned 表示该用户是否已分配该医院；
     *               不传时 assigned 表示全系统任意用户是否已分配
     * @return 模板详情VO，包含基本信息、医院数量及明细列表
     * @throws BusinessException 模板不存在时抛出 TEMPLATE_NOT_FOUND
     */
    @Override
    public HospitalGroupTemplateVO getTemplateById(Long id, Long userId) {
        log.info("根据ID查询医院组合模板详情，id={}, userId={}", id, userId);
        try {
            HospitalGroupTemplateEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院组合模板不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND);
            }
            HospitalGroupTemplateVO vo = toVOWithCount(entity);
            // 填充明细列表（含机构信息和已分配状态）
            List<HospitalGroupTemplateDetailVO> details = getDetails(id, userId);
            vo.setDetails(details);
            log.info("查询医院组合模板详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询医院组合模板详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建医院组合模板
     *
     * @param dto 创建参数，包含模板名称和关联医院ID列表
     * @throws BusinessException 模板名称已存在时抛出 TEMPLATE_EXISTS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(CreateHospitalGroupTemplateDTO dto) {
        log.info("创建医院组合模板，templateName={}", dto.getTemplateName());
        try {
            if (isTemplateNameExists(dto.getTemplateName())) {
                log.warn("模板名称已存在，templateName={}", dto.getTemplateName());
                throw new BusinessException(ErrorCodeEnum.TEMPLATE_EXISTS);
            }
            HospitalGroupTemplateEntity entity = HospitalGroupTemplateConvert.toEntity(dto);
            // 生成唯一模板编号
            entity.setTemplateCode(codeGeneratorService.generate(CodeRuleConstants.TEMPLATE_NO));
            entity.setStatus(StatusConstants.NORMAL);
            save(entity);
            // 批量保存模板关联的医院明细
            saveDetails(entity.getId(), dto.getHospitalIds());
            log.info("创建医院组合模板成功，id={}, templateCode={}, 包含医院数量={}",
                    entity.getId(), entity.getTemplateCode(),
                    dto.getHospitalIds() != null ? dto.getHospitalIds().size() : 0);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建医院组合模板异常，templateName={}", dto.getTemplateName(), e);
            throw e;
        }
    }

    /**
     * 更新医院组合模板（含医院明细的先删后插）
     *
     * @param id  模板ID
     * @param dto 更新参数；hospitalIds 不为 null 时触发明细全量替换
     * @throws BusinessException 模板不存在时抛出 TEMPLATE_NOT_FOUND；名称重复时抛出 TEMPLATE_EXISTS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, UpdateHospitalGroupTemplateDTO dto) {
        log.info("更新医院组合模板，id={}", id);
        try {
            HospitalGroupTemplateEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院组合模板不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND);
            }
            if (StrUtil.isNotBlank(dto.getTemplateName()) && !dto.getTemplateName().equals(entity.getTemplateName())) {
                if (isTemplateNameExistsExcludingId(dto.getTemplateName(), id)) {
                    log.warn("模板名称已存在，templateName={}", dto.getTemplateName());
                    throw new BusinessException(ErrorCodeEnum.TEMPLATE_EXISTS);
                }
            }
            BeanUtils.copyProperties(dto, entity, "id", "templateCode", "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            if (dto.getHospitalIds() != null) {
                // 先删除旧明细，再批量插入新明细（全量替换策略）
                detailMapper.deleteByTemplateId(id);
                saveDetails(id, dto.getHospitalIds());
            }
            log.info("更新医院组合模板成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新医院组合模板异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除医院组合模板（同步删除关联明细）
     *
     * @param id 模板ID
     * @throws BusinessException 模板不存在时抛出 TEMPLATE_NOT_FOUND
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTemplate(Long id) {
        log.info("删除医院组合模板，id={}", id);
        try {
            HospitalGroupTemplateEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院组合模板不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND);
            }
            // 先删除明细，再删除主记录，保证数据一致性
            detailMapper.deleteByTemplateId(id);
            removeById(id);
            log.info("删除医院组合模板成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除医院组合模板异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改医院组合模板启用/禁用状态
     *
     * @param id     模板ID
     * @param status 目标状态（StatusConstants.NORMAL=1 启用，StatusConstants.DISABLED=0 禁用）
     * @throws BusinessException 状态值非法时抛出 PARAM_ERROR；模板不存在时抛出 TEMPLATE_NOT_FOUND
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改医院组合模板状态，id={}, status={}", id, status);
        try {
            if (status == null || (status != StatusConstants.DISABLED && status != StatusConstants.NORMAL)) {
                log.warn("状态值不合法，status={}", status);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            HospitalGroupTemplateEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院组合模板不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改医院组合模板状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改医院组合模板状态异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 获取医院组合模板下拉选项列表（仅返回启用状态的模板）
     *
     * @return 简化VO列表，包含id、名称、编号和医院数量，按名称升序排列
     */
    @Override
    public List<HospitalGroupTemplateSimpleVO> listOptions() {
        log.info("获取医院组合模板下拉选项");
        try {
            LambdaQueryWrapper<HospitalGroupTemplateEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HospitalGroupTemplateEntity::getStatus, StatusConstants.NORMAL)
                    .orderByAsc(HospitalGroupTemplateEntity::getTemplateName);
            List<HospitalGroupTemplateEntity> list = list(wrapper);
            List<HospitalGroupTemplateSimpleVO> voList = list.stream().map(this::toSimpleVO).collect(Collectors.toList());
            log.info("获取医院组合模板下拉选项成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("获取医院组合模板下拉选项异常", e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 将模板实体转换为VO，并附加关联医院数量和状态名称
     *
     * @param entity 模板实体
     * @return 包含医院数量和状态名称的VO
     */
    private HospitalGroupTemplateVO toVOWithCount(HospitalGroupTemplateEntity entity) {
        HospitalGroupTemplateVO vo = new HospitalGroupTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        // 查询该模板关联的启用状态医院数量（过滤已禁用）
        int count = countEnabledHospitals(entity.getId());
        vo.setHospitalCount(count);
        return vo;
    }

    /**
     * 将模板实体转换为简化VO（用于下拉选项）
     *
     * @param entity 模板实体
     * @return 简化VO，仅含id、名称、编号和医院数量
     */
    private HospitalGroupTemplateSimpleVO toSimpleVO(HospitalGroupTemplateEntity entity) {
        HospitalGroupTemplateSimpleVO vo = new HospitalGroupTemplateSimpleVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setTemplateCode(entity.getTemplateCode());
        // 查询该模板关联的启用状态医院数量（过滤已禁用）
        int count = countEnabledHospitals(entity.getId());
        vo.setHospitalCount(count);
        return vo;
    }

    /**
     * 统计模板中启用状态的医院数量
     *
     * @param templateId 模板ID
     * @return 启用状态的医院数量
     */
    private int countEnabledHospitals(Long templateId) {
        List<HospitalGroupTemplateDetailEntity> details = detailMapper.selectList(
                new LambdaQueryWrapper<HospitalGroupTemplateDetailEntity>()
                        .eq(HospitalGroupTemplateDetailEntity::getTemplateId, templateId));
        if (details == null || details.isEmpty()) {
            return 0;
        }
        List<Long> hospitalIds = details.stream()
                .map(HospitalGroupTemplateDetailEntity::getHospitalId)
                .collect(Collectors.toList());
        return (int) orgService.listByIds(hospitalIds).stream()
                .filter(org -> org.getStatus() != null && org.getStatus().equals(StatusConstants.NORMAL))
                .count();
    }

    /**
     * 查询模板医院明细列表，并填充机构详细信息和已分配状态
     * <p>
     * assigned 字段语义：
     * - userId 为 null：表示该医院已被全系统任意用户分配（模板管理场景）
     * - userId 不为 null：表示该医院已被指定用户分配（用户分配预览场景）
     * </p>
     *
     * @param templateId 模板ID
     * @param userId     用户ID（可选）
     * @return 明细VO列表，每条记录包含机构信息和 assigned 标志
     */
    private List<HospitalGroupTemplateDetailVO> getDetails(Long templateId, Long userId) {
        List<HospitalGroupTemplateDetailEntity> details = detailMapper.selectList(
                new LambdaQueryWrapper<HospitalGroupTemplateDetailEntity>()
                        .eq(HospitalGroupTemplateDetailEntity::getTemplateId, templateId));
        if (details == null || details.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> hospitalIds = details.stream()
                .map(HospitalGroupTemplateDetailEntity::getHospitalId)
                .collect(Collectors.toList());

        // 批量查询机构信息（1次 IN 查询，避免 N+1）
        Map<Long, OrgEntity> orgMap = orgService.listByIds(hospitalIds).stream()
                .collect(Collectors.toMap(OrgEntity::getId, o -> o));

        // 根据 userId 参数选择查询策略
        Set<Long> assignedHospitalIds;
        if (userId != null) {
            // 用户分配场景：查询该用户已分配的医院ID集合
            List<Long> userHospitalIds = userHospitalService.getHospitalIdsByUserId(userId);
            assignedHospitalIds = userHospitalIds.stream().collect(Collectors.toSet());
        } else {
            // 模板管理场景：查询全系统任意用户已分配的医院ID集合
            assignedHospitalIds = userHospitalService.getAssignedHospitalIds(hospitalIds);
        }

        return details.stream().map(d -> {
            HospitalGroupTemplateDetailVO detailVO = new HospitalGroupTemplateDetailVO();
            detailVO.setId(d.getId());
            detailVO.setTemplateId(d.getTemplateId());
            detailVO.setHospitalId(d.getHospitalId());
            detailVO.setCreateTime(d.getCreateTime());
            // 若该医院ID存在于已分配集合中，则标记为已分配
            detailVO.setAssigned(assignedHospitalIds.contains(d.getHospitalId()));
            OrgEntity org = orgMap.get(d.getHospitalId());
            if (org != null) {
                detailVO.setHospitalName(org.getOrgName());
                detailVO.setHospitalCode(org.getOrgCode());
                detailVO.setFullAreaName(org.getAreaName());
                detailVO.setHospitalLevelName(null);
                detailVO.setContact(org.getContact());
                detailVO.setPhone(org.getPhone());
            }
            return detailVO;
        })
        .filter(vo -> {
            // 过滤掉已禁用的医院（保留关联数据，但查询时不返回）
            OrgEntity org = orgMap.get(vo.getHospitalId());
            return org != null && org.getStatus() != null && org.getStatus().equals(StatusConstants.NORMAL);
        })
        .collect(Collectors.toList());
    }

    /**
     * 批量保存模板医院明细记录
     * <p>
     * 使用 insertBatch 替代逐条 insert，消除 N+1 写入问题。
     * 写入前校验所有 hospitalId 均为有效启用的医疗机构（orgType=1.3）。
     * </p>
     *
     * @param templateId  模板ID
     * @param hospitalIds 关联的医院ID列表；为空时直接返回
     */
    private void saveDetails(Long templateId, List<Long> hospitalIds) {
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            return;
        }
        // 校验所有 hospitalId 均为有效医疗机构
        List<OrgEntity> orgs = orgService.listByIds(hospitalIds);
        boolean hasInvalid = orgs.stream().anyMatch(o -> !com.yigongbao.common.constant.DictCodeConstants.ORG_TYPE_HOSPITAL.equals(o.getOrgType()));
        if (hasInvalid || orgs.size() != new java.util.HashSet<>(hospitalIds).size()) {
            throw new com.yigongbao.common.exception.BusinessException(com.yigongbao.common.enums.ErrorCodeEnum.HOSPITAL_NOT_FOUND);
        }
        List<HospitalGroupTemplateDetailEntity> details = new ArrayList<>(hospitalIds.size());
        for (Long hospitalId : hospitalIds) {
            HospitalGroupTemplateDetailEntity detail = new HospitalGroupTemplateDetailEntity();
            detail.setTemplateId(templateId);
            detail.setHospitalId(hospitalId);
            details.add(detail);
        }
        // 批量插入所有明细，单条 SQL 替代多次 insert
        detailMapper.insertBatch(details);
    }

    /**
     * 检查模板名称是否已存在
     *
     * @param templateName 待检查的模板名称
     * @return true 表示名称已被占用
     */
    private boolean isTemplateNameExists(String templateName) {
        return count(new LambdaQueryWrapper<HospitalGroupTemplateEntity>()
                .eq(HospitalGroupTemplateEntity::getTemplateName, templateName)) > 0;
    }

    /**
     * 检查模板名称是否已被其他模板占用（排除自身）
     *
     * @param templateName 待检查的模板名称
     * @param excludeId    排除的模板ID（即当前正在更新的模板）
     * @return true 表示名称已被其他模板占用
     */
    private boolean isTemplateNameExistsExcludingId(String templateName, Long excludeId) {
        return count(new LambdaQueryWrapper<HospitalGroupTemplateEntity>()
                .eq(HospitalGroupTemplateEntity::getTemplateName, templateName)
                .ne(HospitalGroupTemplateEntity::getId, excludeId)) > 0;
    }
}
