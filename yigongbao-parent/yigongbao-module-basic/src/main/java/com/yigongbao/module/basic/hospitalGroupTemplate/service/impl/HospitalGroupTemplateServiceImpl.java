package com.yigongbao.module.basic.hospitalGroupTemplate.service.impl;

/**
 * 医院组合模板 Service 实现类
 * 处理医院组合模板的 CRUD 和状态管理等业务逻辑
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
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospitalGroupTemplate.convert.HospitalGroupTemplateConvert;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateDetailEntity;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import com.yigongbao.module.basic.hospitalGroupTemplate.mapper.HospitalGroupTemplateDetailMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.mapper.HospitalGroupTemplateMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 医院组合模板 Service 实现类
 * 处理医院组合模板的 CRUD 和状态管理等业务逻辑
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalGroupTemplateServiceImpl extends ServiceImpl<HospitalGroupTemplateMapper, HospitalGroupTemplateEntity>
        implements HospitalGroupTemplateService {

    private final HospitalGroupTemplateDetailMapper detailMapper;
    private final HospitalMapper hospitalMapper;
    private final HospitalService hospitalService;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * 分页查询医院组合模板列表
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param templateName 模板名称（模糊查询）
     * @param status 状态
     * @return 分页后的模板列表
     */
    @Override
    public IPage<HospitalGroupTemplateVO> listTemplate(Integer pageNum, Integer pageSize, String templateName, Integer status) {
        log.info("分页查询医院组合模板列表，pageNum={}, pageSize={}, templateName={}, status={}",
                pageNum, pageSize, templateName, status);
        try {
            Page<HospitalGroupTemplateEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<HospitalGroupTemplateEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(templateName), HospitalGroupTemplateEntity::getTemplateName, templateName)
                    .eq(Objects.nonNull(status), HospitalGroupTemplateEntity::getStatus, status)
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
     * 根据ID查询医院组合模板详情
     *
     * @param id 模板ID
     * @return 模板详情（包含医院明细列表）
     * @throws BusinessException 模板不存在
     */
    @Override
    public HospitalGroupTemplateVO getTemplateById(Long id) {
        log.info("根据ID查询医院组合模板详情，id={}", id);
        try {
            HospitalGroupTemplateEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院组合模板不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND);
            }
            HospitalGroupTemplateVO vo = toVOWithCount(entity);
            List<HospitalGroupTemplateDetailVO> details = getDetails(id);
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
     * @param dto 创建参数
     * @throws BusinessException 模板名称已存在
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
            entity.setTemplateCode(codeGeneratorService.generate(CodeRuleConstants.TEMPLATE_NO));
            entity.setStatus(StatusConstants.NORMAL);
            save(entity);
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
     * 更新医院组合模板
     *
     * @param id 模板ID
     * @param dto 更新参数
     * @throws BusinessException 模板不存在或名称已存在
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
     * 删除医院组合模板
     *
     * @param id 模板ID
     * @throws BusinessException 模板不存在
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
     * 修改医院组合模板状态
     *
     * @param id 模板ID
     * @param status 状态（0=禁用，1=正常）
     * @throws BusinessException 模板不存在或状态值不合法
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
     * 获取医院组合模板下拉选项
     *
     * @param status 状态筛选（可选）
     * @return 模板下拉列表
     */
    @Override
    public List<HospitalGroupTemplateSimpleVO> listOptions(Integer status) {
        log.info("获取医院组合模板下拉选项，status={}", status);
        try {
            LambdaQueryWrapper<HospitalGroupTemplateEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Objects.nonNull(status), HospitalGroupTemplateEntity::getStatus, status)
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
     * 转换为VO并填充医院数量
     *
     * @param entity 模板实体
     * @return 模板VO
     */
    private HospitalGroupTemplateVO toVOWithCount(HospitalGroupTemplateEntity entity) {
        HospitalGroupTemplateVO vo = new HospitalGroupTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        Long count = detailMapper.countByTemplateId(entity.getId());
        vo.setHospitalCount(count != null ? count.intValue() : 0);
        return vo;
    }

    /**
     * 转换为简洁VO（用于下拉选项）
     *
     * @param entity 模板实体
     * @return 简洁模板VO
     */
    private HospitalGroupTemplateSimpleVO toSimpleVO(HospitalGroupTemplateEntity entity) {
        HospitalGroupTemplateSimpleVO vo = new HospitalGroupTemplateSimpleVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setTemplateCode(entity.getTemplateCode());
        Long count = detailMapper.countByTemplateId(entity.getId());
        vo.setHospitalCount(count != null ? count.intValue() : 0);
        return vo;
    }

    /**
     * 查询模板医院明细并填充医院详细信息
     *
     * @param templateId 模板ID
     * @return 明细VO列表
     */
    private List<HospitalGroupTemplateDetailVO> getDetails(Long templateId) {
        List<HospitalGroupTemplateDetailEntity> details = detailMapper.selectList(
                new LambdaQueryWrapper<HospitalGroupTemplateDetailEntity>()
                        .eq(HospitalGroupTemplateDetailEntity::getTemplateId, templateId));
        if (details == null || details.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> hospitalIds = details.stream()
                .map(HospitalGroupTemplateDetailEntity::getHospitalId)
                .collect(Collectors.toList());
        List<HospitalEntity> hospitals = hospitalMapper.selectBatchIds(hospitalIds);
        return details.stream().map(d -> {
            HospitalGroupTemplateDetailVO detailVO = new HospitalGroupTemplateDetailVO();
            detailVO.setId(d.getId());
            detailVO.setTemplateId(d.getTemplateId());
            detailVO.setHospitalId(d.getHospitalId());
            detailVO.setCreateTime(d.getCreateTime());
            hospitals.stream()
                    .filter(h -> Objects.equals(h.getId(), d.getHospitalId()))
                    .findFirst()
                    .ifPresent(h -> {
                        detailVO.setHospitalName(h.getHospitalName());
                        detailVO.setHospitalCode(h.getHospitalCode());
                        detailVO.setFullAreaName(h.getFullAreaName());
                        // hospitalLevelName 需通过字典服务获取（字典服务在 system 模块，basic 不依赖），由 Controller 层负责填充
                        detailVO.setHospitalLevelName(null);
                        detailVO.setContact(h.getContact());
                        detailVO.setPhone(h.getPhone());
                    });
            return detailVO;
        }).collect(Collectors.toList());
    }

    /**
     * 批量保存模板医院明细
     *
     * @param templateId 模板ID
     * @param hospitalIds 医院ID列表
     */
    private void saveDetails(Long templateId, List<Long> hospitalIds) {
        if (hospitalIds == null || hospitalIds.isEmpty()) {
            return;
        }
        List<HospitalGroupTemplateDetailEntity> details = new ArrayList<>(hospitalIds.size());
        for (Long hospitalId : hospitalIds) {
            HospitalGroupTemplateDetailEntity detail = new HospitalGroupTemplateDetailEntity();
            detail.setTemplateId(templateId);
            detail.setHospitalId(hospitalId);
            details.add(detail);
        }
        // 使用批量插入替代逐条 insert，消除 N+1 查询
        detailMapper.insertBatch(details);
    }

    /**
     * 校验模板名称是否存在
     *
     * @param templateName 模板名称
     * @return true-存在，false-不存在
     */
    private boolean isTemplateNameExists(String templateName) {
        return count(new LambdaQueryWrapper<HospitalGroupTemplateEntity>()
                .eq(HospitalGroupTemplateEntity::getTemplateName, templateName)) > 0;
    }

    /**
     * 校验模板名称是否存在（排除指定ID）
     *
     * @param templateName 模板名称
     * @param excludeId 排除的模板ID
     * @return true-存在，false-不存在
     */
    private boolean isTemplateNameExistsExcludingId(String templateName, Long excludeId) {
        return count(new LambdaQueryWrapper<HospitalGroupTemplateEntity>()
                .eq(HospitalGroupTemplateEntity::getTemplateName, templateName)
                .ne(HospitalGroupTemplateEntity::getId, excludeId)) > 0;
    }
}
