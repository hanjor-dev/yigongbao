package com.yigongbao.module.basic.code.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.CodeResetTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.convert.CodeRuleConvert;
import com.yigongbao.module.basic.code.dto.CreateCodeRuleDTO;
import com.yigongbao.module.basic.code.dto.UpdateCodeRuleDTO;
import com.yigongbao.module.basic.code.entity.CodeRuleEntity;
import com.yigongbao.module.basic.code.mapper.CodeRuleMapper;
import com.yigongbao.module.basic.code.service.CodeRuleService;
import com.yigongbao.module.basic.code.vo.CodeRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 编码规则 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeRuleServiceImpl extends ServiceImpl<CodeRuleMapper, CodeRuleEntity> implements CodeRuleService {

    /**
     * 分页查询编码规则
     */
    @Override
    public IPage<CodeRuleVO> listRules(Integer pageNum, Integer pageSize, String ruleCode, String ruleName, Integer status) {
        log.info("分页查询编码规则，pageNum={}, pageSize={}, ruleCode={}, ruleName={}, status={}",
                pageNum, pageSize, ruleCode, ruleName, status);
        try {
            Page<CodeRuleEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<CodeRuleEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Objects.nonNull(ruleCode) && !ruleCode.isEmpty(),
                    CodeRuleEntity::getRuleCode, ruleCode)
                    .like(Objects.nonNull(ruleName) && !ruleName.isEmpty(),
                            CodeRuleEntity::getRuleName, ruleName)
                    .eq(Objects.nonNull(status), CodeRuleEntity::getStatus, status)
                    .orderByDesc(CodeRuleEntity::getCreateTime);

            IPage<CodeRuleEntity> pageResult = page(page, wrapper);

            IPage<CodeRuleVO> voPage = pageResult.convert(entity -> {
                CodeRuleVO vo = CodeRuleConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            });

            log.info("分页查询编码规则成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询编码规则异常", e);
            throw e;
        }
    }

    /**
     * 根据规则编码查询
     */
    @Override
    public CodeRuleVO getByRuleCode(String ruleCode) {
        log.info("根据规则编码查询，ruleCode={}", ruleCode);
        try {
            CodeRuleEntity entity = getOne(new LambdaQueryWrapper<CodeRuleEntity>()
                    .eq(CodeRuleEntity::getRuleCode, ruleCode));
            if (entity == null) {
                log.warn("编码规则不存在，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            CodeRuleVO vo = CodeRuleConvert.toVO(entity);
            fillExtraFields(vo, entity);
            log.info("查询编码规则成功，id={}", entity.getId());
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询编码规则异常，ruleCode={}", ruleCode, e);
            throw e;
        }
    }

    /**
     * 创建编码规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRule(CreateCodeRuleDTO dto) {
        log.info("创建编码规则，ruleCode={}", dto.getRuleCode());
        try {
            if (isRuleCodeExists(dto.getRuleCode(), null)) {
                log.warn("规则编码已存在，ruleCode={}", dto.getRuleCode());
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_EXISTS);
            }

            CodeRuleEntity entity = CodeRuleConvert.toEntity(dto);
            if (entity.getSeqLength() == null) {
                entity.setSeqLength(6);
            }
            if (entity.getResetType() == null) {
                entity.setResetType(CodeResetTypeEnum.NEVER.getCode());
            }
            if (entity.getStep() == null) {
                entity.setStep(1);
            }
            if (entity.getStatus() == null) {
                entity.setStatus(StatusConstants.NORMAL);
            }

            save(entity);
            log.info("创建编码规则成功，id={}, ruleCode={}", entity.getId(), dto.getRuleCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建编码规则异常，ruleCode={}", dto.getRuleCode(), e);
            throw e;
        }
    }

    /**
     * 更新编码规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, UpdateCodeRuleDTO dto) {
        log.info("更新编码规则，id={}", id);
        try {
            CodeRuleEntity entity = getById(id);
            if (entity == null) {
                log.warn("编码规则不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }

            BeanUtils.copyProperties(dto, entity, "id", "ruleCode", "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新编码规则成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新编码规则异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除编码规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRule(Long id) {
        log.info("删除编码规则，id={}", id);
        try {
            CodeRuleEntity entity = getById(id);
            if (entity == null) {
                log.warn("编码规则不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            removeById(id);
            log.info("删除编码规则成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除编码规则异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 填充额外字段
     */
    private void fillExtraFields(CodeRuleVO vo, CodeRuleEntity entity) {
        if (entity.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        }
        if (entity.getResetType() != null) {
            for (CodeResetTypeEnum resetType : CodeResetTypeEnum.values()) {
                if (resetType.getCode().equals(entity.getResetType())) {
                    vo.setResetTypeName(resetType.getName());
                    break;
                }
            }
        }
    }

    /**
     * 校验规则编码是否存在
     */
    private boolean isRuleCodeExists(String ruleCode, Long excludeId) {
        LambdaQueryWrapper<CodeRuleEntity> wrapper = new LambdaQueryWrapper<CodeRuleEntity>()
                .eq(CodeRuleEntity::getRuleCode, ruleCode);
        if (excludeId != null) {
            wrapper.ne(CodeRuleEntity::getId, excludeId);
        }
        return count(wrapper) > 0;
    }
}
