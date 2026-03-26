package com.yigongbao.module.basic.code.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.CodeResetTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.entity.CodeRuleEntity;
import com.yigongbao.module.basic.code.entity.CodeSequenceEntity;
import com.yigongbao.module.basic.code.mapper.CodeRuleMapper;
import com.yigongbao.module.basic.code.mapper.CodeSequenceMapper;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 编码生成服务实现类
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeGeneratorServiceImpl implements CodeGeneratorService {

    private final CodeRuleMapper codeRuleMapper;
    private final CodeSequenceMapper codeSequenceMapper;

    /**
     * 生成编码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generate(String ruleCode) {
        log.info("生成编码，ruleCode={}", ruleCode);
        try {
            // 1. 获取规则配置
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            if (rule == null) {
                log.warn("编码规则不存在，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            if (rule.getStatus() != null && rule.getStatus().equals(StatusConstants.DISABLED)) {
                log.warn("编码规则已禁用，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_DISABLED);
            }

            // 2. 获取或创建序号记录
            CodeSequenceEntity sequence = getOrCreateSequence(ruleCode);

            // 3. 检查是否需要重置
            checkAndReset(rule, sequence);

            // 4. 递增序号（使用乐观锁版本控制，并发更新时重试）
            long newSeq = sequence.getCurrentSeq() + (rule.getStep() != null ? rule.getStep() : 1);
            int currentVersion = sequence.getVersion();
            // 使用 LambdaUpdateWrapper 替代 updateById，避免 MyBatis-Plus OptimisticLockerInterceptor 参数注入问题
            LambdaUpdateWrapper<CodeSequenceEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CodeSequenceEntity::getId, sequence.getId())
                    .eq(CodeSequenceEntity::getVersion, currentVersion)
                    .set(CodeSequenceEntity::getCurrentSeq, newSeq)
                    .set(CodeSequenceEntity::getVersion, currentVersion + 1);
            int rows = codeSequenceMapper.update(null, updateWrapper);
            if (rows == 0) {
                log.warn("编码序号更新冲突，重试生成，ruleCode={}", ruleCode);
                return generate(ruleCode);
            }

            // 5. 组装编码
            String code = buildCode(rule, newSeq);
            log.info("生成编码成功，ruleCode={}, code={}", ruleCode, code);
            return code;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成编码异常，ruleCode={}", ruleCode, e);
            throw new BusinessException(ErrorCodeEnum.CODE_GENERATE_FAILED);
        }
    }

    /**
     * 生成带业务前缀的编码
     *
     * @param ruleCode 规则编码
     * @param bizPrefix 业务前缀
     * @return 带前缀的编码
     */
    @Override
    public String generateWithBizPrefix(String ruleCode, String bizPrefix) {
        log.info("生成带业务前缀的编码，ruleCode={}, bizPrefix={}", ruleCode, bizPrefix);
        try {
            String baseCode = generate(ruleCode);
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            if (rule == null) {
                log.warn("编码规则不存在，生成失败，返回基础编码，ruleCode={}", ruleCode);
                return baseCode;
            }
            String result = rule.getPrefix() + bizPrefix + "-" + baseCode;
            log.info("生成带业务前缀的编码成功，ruleCode={}, bizPrefix={}, result={}", ruleCode, bizPrefix, result);
            return result;
        } catch (Exception e) {
            log.error("生成带业务前缀的编码异常，ruleCode={}, bizPrefix={}", ruleCode, bizPrefix, e);
            throw e;
        }
    }

    @Override
    public String generateWithCustomPrefix(String ruleCode, String prefix) {
        log.info("生成带自定义前缀的编码，ruleCode={}, prefix={}", ruleCode, prefix);
        try {
            // 生成基础序号
            String seq = generate(ruleCode);
            // 拼接前缀
            String result = prefix + seq;
            log.info("生成带自定义前缀的编码成功，ruleCode={}, prefix={}, result={}", ruleCode, prefix, result);
            return result;
        } catch (Exception e) {
            log.error("生成带自定义前缀的编码异常，ruleCode={}, prefix={}", ruleCode, prefix, e);
            throw e;
        }
    }

    /**
     * 生成带序号后缀的编码
     * 用于同一业务前缀下生成多个子编码，如数据包编码：202603250001-1、202603250001-2
     * 每个 bizKey 有独立的序号池，互不影响
     *
     * @param ruleCode 规则编码
     * @param bizKey 业务标识（如订单编号，用于隔离序号池）
     * @return 带序号后缀的编码（如 202603250001-1）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateWithSeqSuffix(String ruleCode, String bizKey) {
        log.info("生成带序号后缀的编码，ruleCode={}, bizKey={}", ruleCode, bizKey);
        try {
            // 1. 获取规则配置
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            if (rule == null) {
                log.warn("编码规则不存在，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            if (rule.getStatus() != null && rule.getStatus().equals(StatusConstants.DISABLED)) {
                log.warn("编码规则已禁用，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_DISABLED);
            }

            // 2. 获取或创建序号记录（按 bizKey 隔离）
            CodeSequenceEntity sequence = getOrCreateSequenceWithBizKey(ruleCode, bizKey);

            // 3. 检查是否需要重置
            checkAndReset(rule, sequence);

            // 4. 递增序号（使用乐观锁版本控制，并发更新时重试）
            long newSeq = sequence.getCurrentSeq() + (rule.getStep() != null ? rule.getStep() : 1);
            int currentVersion = sequence.getVersion();
            // 使用 LambdaUpdateWrapper 替代 updateById，避免 MyBatis-Plus OptimisticLockerInterceptor 参数注入问题
            LambdaUpdateWrapper<CodeSequenceEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CodeSequenceEntity::getId, sequence.getId())
                    .eq(CodeSequenceEntity::getVersion, currentVersion)
                    .set(CodeSequenceEntity::getCurrentSeq, newSeq)
                    .set(CodeSequenceEntity::getVersion, currentVersion + 1);
            int rows = codeSequenceMapper.update(null, updateWrapper);
            if (rows == 0) {
                log.warn("编码序号更新冲突，重试生成，ruleCode={}, bizKey={}", ruleCode, bizKey);
                return generateWithSeqSuffix(ruleCode, bizKey);
            }

            // 5. 组装编码：业务前缀 + "-" + 序号（序号不带补零）
            String result = bizKey + "-" + newSeq;
            log.info("生成带序号后缀的编码成功，ruleCode={}, bizKey={}, result={}", ruleCode, bizKey, result);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成带序号后缀的编码异常，ruleCode={}, bizKey={}", ruleCode, bizKey, e);
            throw new BusinessException(ErrorCodeEnum.CODE_GENERATE_FAILED);
        }
    }

    /**
     * 预览编码格式
     *
     * @param ruleCode 规则编码
     * @return 编码预览
     * @throws BusinessException 规则不存在
     */
    @Override
    public String preview(String ruleCode) {
        log.info("预览编码格式，ruleCode={}", ruleCode);
        try {
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            if (rule == null) {
                log.warn("编码规则不存在，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            String result = buildCode(rule, 1L).replace("1", "N");
            log.info("预览编码格式成功，ruleCode={}, preview={}", ruleCode, result);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("预览编码格式异常，ruleCode={}", ruleCode, e);
            throw e;
        }
    }

    /**
     * 获取或创建序号记录
     */
    private CodeSequenceEntity getOrCreateSequence(String ruleCode) {
        CodeSequenceEntity sequence = codeSequenceMapper.selectByRuleCode(ruleCode);
        if (sequence == null) {
            sequence = new CodeSequenceEntity();
            sequence.setRuleCode(ruleCode);
            sequence.setCurrentSeq(0L);
            sequence.setLastDate(LocalDate.now());
            sequence.setVersion(0);
            codeSequenceMapper.insert(sequence);
        }
        return sequence;
    }

    /**
     * 获取或创建序号记录（支持业务标识隔离）
     *
     * @param ruleCode 规则编码
     * @param bizKey 业务标识
     * @return 序号记录
     */
    private CodeSequenceEntity getOrCreateSequenceWithBizKey(String ruleCode, String bizKey) {
        CodeSequenceEntity sequence = codeSequenceMapper.selectByRuleCodeAndBizKey(ruleCode, bizKey);
        if (sequence == null) {
            sequence = new CodeSequenceEntity();
            sequence.setRuleCode(ruleCode);
            sequence.setBizKey(bizKey);
            sequence.setCurrentSeq(0L);
            sequence.setLastDate(LocalDate.now());
            sequence.setVersion(0);
            codeSequenceMapper.insert(sequence);
        }
        return sequence;
    }

    /**
     * 检查并执行重置
     *
     * @param rule 编码规则
     * @param sequence 序号记录
     */
    private void checkAndReset(CodeRuleEntity rule, CodeSequenceEntity sequence) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = sequence.getLastDate();
        String resetType = rule.getResetType();

        boolean needReset = false;
        if (StringUtils.hasText(resetType)) {
            if (resetType.equals(CodeResetTypeEnum.DAY.getCode())) {
                needReset = lastDate == null || !lastDate.equals(today);
            } else if (resetType.equals(CodeResetTypeEnum.MONTH.getCode())) {
                needReset = lastDate == null || lastDate.getYear() != today.getYear() || lastDate.getMonthValue() != today.getMonthValue();
            } else if (resetType.equals(CodeResetTypeEnum.YEAR.getCode())) {
                needReset = lastDate == null || lastDate.getYear() != today.getYear();
            }
        }

        if (needReset) {
            sequence.setCurrentSeq(0L);
            sequence.setLastDate(today);
            // 使用 LambdaUpdateWrapper 替代 updateById
            LambdaUpdateWrapper<CodeSequenceEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CodeSequenceEntity::getId, sequence.getId())
                    .set(CodeSequenceEntity::getCurrentSeq, 0L)
                    .set(CodeSequenceEntity::getLastDate, today);
            codeSequenceMapper.update(null, updateWrapper);
            log.info("编码序号已重置，ruleCode={}, lastDate={}", rule.getRuleCode(), today);
        }
    }

    /**
     * 组装编码
     */
    private String buildCode(CodeRuleEntity rule, Long seq) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.hasText(rule.getPrefix())) {
            sb.append(rule.getPrefix());
        }

        if (StringUtils.hasText(rule.getDateFormat())) {
            sb.append(formatDate(rule.getDateFormat()));
        }

        sb.append(formatSeq(rule.getSeqLength() != null ? rule.getSeqLength() : 6, seq));

        return sb.toString();
    }

    /**
     * 格式化日期
     */
    private String formatDate(String format) {
        LocalDateTime now = LocalDateTime.now();
        return format
                .replace("{yyyy}", String.format("%04d", now.getYear()))
                .replace("{MM}", String.format("%02d", now.getMonthValue()))
                .replace("{dd}", String.format("%02d", now.getDayOfMonth()))
                .replace("{HH}", String.format("%02d", now.getHour()))
                .replace("{mm}", String.format("%02d", now.getMinute()))
                .replace("{ss}", String.format("%02d", now.getSecond()));
    }

    /**
     * 格式化序号
     */
    private String formatSeq(int length, Long seq) {
        String seqStr = String.valueOf(seq);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - seqStr.length(); i++) {
            sb.append('0');
        }
        sb.append(seqStr);
        return sb.toString();
    }
}
