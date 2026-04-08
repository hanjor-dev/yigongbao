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

    private static final int MAX_RETRY_COUNT = 5;

    private final CodeRuleMapper codeRuleMapper;
    private final CodeSequenceMapper codeSequenceMapper;

    /**
     * 生成编码（内部方法，支持重试）
     *
     * @param ruleCode 规则编码
     * @param retryCount 当前重试次数
     * @return 生成的编码
     */
    private String generateInternal(String ruleCode, int retryCount) {
        log.info("生成编码，ruleCode={}", ruleCode);
        try {
            // 1. 获取规则配置
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            if (rule == null) {
                log.warn("编码规则不存在，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            if (Integer.valueOf(StatusConstants.DISABLED).equals(rule.getStatus())) {
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
                log.warn("编码序号更新冲突，尝试重新生成，ruleCode={}, retryCount={}", ruleCode, retryCount);
                if (retryCount >= MAX_RETRY_COUNT) {
                    log.error("编码序号重试次数超限，ruleCode={}, maxRetries={}", ruleCode, MAX_RETRY_COUNT);
                    throw new BusinessException(ErrorCodeEnum.CODE_GENERATE_FAILED);
                }
                return generateInternal(ruleCode, retryCount + 1);
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
     * 生成带序号后缀的编码（内部方法，支持重试）
     *
     * @param ruleCode 规则编码
     * @param bizKey 业务标识（如订单编号，用于隔离序号池）
     * @param retryCount 当前重试次数
     * @return 生成的编码
     */
    private String generateWithSeqSuffixInternal(String ruleCode, String bizKey, int retryCount) {
        log.info("生成带序号后缀的编码，ruleCode={}, bizKey={}", ruleCode, bizKey);
        try {
            // 1. 获取规则配置
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            if (rule == null) {
                log.warn("编码规则不存在，ruleCode={}", ruleCode);
                throw new BusinessException(ErrorCodeEnum.CODE_RULE_NOT_FOUND);
            }
            if (Integer.valueOf(StatusConstants.DISABLED).equals(rule.getStatus())) {
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
                log.warn("编码序号更新冲突，尝试重新生成，ruleCode={}, bizKey={}, retryCount={}", ruleCode, bizKey, retryCount);
                if (retryCount >= MAX_RETRY_COUNT) {
                    log.error("编码序号重试次数超限，ruleCode={}, bizKey={}, maxRetries={}", ruleCode, bizKey, MAX_RETRY_COUNT);
                    throw new BusinessException(ErrorCodeEnum.CODE_GENERATE_FAILED);
                }
                return generateWithSeqSuffixInternal(ruleCode, bizKey, retryCount + 1);
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
     * 生成编码
     * 将内部技术性异常（编码规则不存在/已禁用/生成失败）翻译为业务友好错误消息后抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generate(String ruleCode) {
        try {
            return generateInternal(ruleCode, 0);
        } catch (BusinessException e) {
            // 将内部技术性错误翻译为用户可理解的业务错误消息
            throw translateToUserMessage(e, "业务编号", null);
        }
    }

    /**
     * 生成带序号后缀的编码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateWithSeqSuffix(String ruleCode, String bizKey) {
        if (!StringUtils.hasText(bizKey)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "bizKey");
        }
        try {
            return generateWithSeqSuffixInternal(ruleCode, bizKey, 0);
        } catch (BusinessException e) {
            throw translateToUserMessage(e, "业务编号", bizKey);
        }
    }

    /**
     * 统一的业务友好错误消息转换
     *
     * @param e 内部抛出的业务异常
     * @param businessName 业务对象名称（用于生成用户友好的错误消息）
     * @param bizKey 业务标识（可选，用于日志上下文）
     * @return 用户友好的业务异常
     */
    private BusinessException translateToUserMessage(BusinessException e, String businessName, String bizKey) {
        int code = e.getCode();
        String msg;
        if (code == ErrorCodeEnum.CODE_RULE_NOT_FOUND.getCode()) {
            msg = "系统编码配置异常，" + businessName + "生成失败，请联系管理员处理";
        } else if (code == ErrorCodeEnum.CODE_RULE_DISABLED.getCode()) {
            msg = "系统编码服务暂时不可用，" + businessName + "生成失败，请稍后重试";
        } else if (code == ErrorCodeEnum.CODE_GENERATE_FAILED.getCode()) {
            msg = businessName + "生成失败（系统繁忙），请稍后重试";
        } else if (code == ErrorCodeEnum.CODE_SEQ_OVERFLOW.getCode()) {
            msg = e.getMessage();
        } else {
            msg = e.getMessage();
        }
        log.debug("编码生成业务友好消息转换，原始code={} bizKey={} -> {}", code, bizKey, msg);
        return new BusinessException(code, msg);
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
            // baseCode 已包含 rule.prefix，无需重复拼接
            // 格式：{bizPrefix}-{prefix}{date?}{seq}（无 prefix 时：{bizPrefix}-{seq}）
            String result = bizPrefix + "-" + baseCode;
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
            // 从 sys_code_rule.current_value 读取初始序号，避免与已有数据编号冲突
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            long initSeq = (rule != null && rule.getCurrentValue() != null) ? rule.getCurrentValue() : 0L;
            sequence.setCurrentSeq(initSeq);
            sequence.setLastDate(LocalDate.now());
            sequence.setVersion(0);
            codeSequenceMapper.insert(sequence);
            // 重新查询以确认 ID 已回写（解决并发插入场景下 ID 可能未回写的问题）
            if (sequence.getId() == null) {
                sequence = codeSequenceMapper.selectByRuleCode(ruleCode);
            }
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
            // 从 sys_code_rule.current_value 读取初始序号（用于已有数据场景）
            CodeRuleEntity rule = codeRuleMapper.selectByRuleCode(ruleCode);
            long initSeq = (rule != null && rule.getCurrentValue() != null) ? rule.getCurrentValue() : 0L;
            sequence.setCurrentSeq(initSeq);
            sequence.setLastDate(LocalDate.now());
            sequence.setVersion(0);
            codeSequenceMapper.insert(sequence);
            // 重新查询以确认 ID 已回写（解决并发插入场景下 ID 可能未回写的问题）
            if (sequence.getId() == null) {
                sequence = codeSequenceMapper.selectByRuleCodeAndBizKey(ruleCode, bizKey);
            }
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
        // 序号超出可表示范围时抛出明确错误，避免生成畸形编码
        long maxSeq = (long) Math.pow(10, length) - 1;
        if (seq > maxSeq) {
            log.error("序号溢出，seqLength={}, seq={}, maxSeq={}", length, seq, maxSeq);
            throw new BusinessException(ErrorCodeEnum.CODE_SEQ_OVERFLOW);
        }
        String seqStr = String.valueOf(seq);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - seqStr.length(); i++) {
            sb.append('0');
        }
        sb.append(seqStr);
        return sb.toString();
    }
}
