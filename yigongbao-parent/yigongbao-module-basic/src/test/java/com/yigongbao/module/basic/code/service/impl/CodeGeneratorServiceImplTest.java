package com.yigongbao.module.basic.code.service.impl;

import com.yigongbao.common.enums.CodeResetTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.entity.CodeRuleEntity;
import com.yigongbao.module.basic.code.entity.CodeSequenceEntity;
import com.yigongbao.module.basic.code.mapper.CodeRuleMapper;
import com.yigongbao.module.basic.code.mapper.CodeSequenceMapper;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 编码生成服务单元测试
 *
 * @author hanjor
 * @date 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("CodeGeneratorService 单元测试")
class CodeGeneratorServiceImplTest {

    @Mock
    private CodeRuleMapper codeRuleMapper;

    @Mock
    private CodeSequenceMapper codeSequenceMapper;

    @InjectMocks
    private CodeGeneratorServiceImpl codeGeneratorService;

    private CodeRuleEntity testRule;
    private CodeSequenceEntity testSequence;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testRule = new CodeRuleEntity();
        testRule.setId(1L);
        testRule.setRuleCode("ORDER_NO");
        testRule.setRuleName("订单编号");
        testRule.setPrefix("ORD-");
        testRule.setDateFormat("{yyyy}{MM}{dd}");
        testRule.setSeqLength(6);
        testRule.setResetType(CodeResetTypeEnum.DAY.getCode());
        testRule.setStep(1);
        testRule.setStatus(1);

        testSequence = new CodeSequenceEntity();
        testSequence.setId(1L);
        testSequence.setRuleCode("ORDER_NO");
        testSequence.setCurrentSeq(100L);
        testSequence.setLastDate(LocalDate.now());
        testSequence.setVersion(0);
    }

    @Test
    @DisplayName("generate: 规则不存在时抛出异常")
    void generate_whenRuleNotExists_shouldThrowException() {
        when(codeRuleMapper.selectByRuleCode("ORDER_NO")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> codeGeneratorService.generate("ORDER_NO")
        );
        assertEquals(ErrorCodeEnum.CODE_RULE_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("generate: 规则已禁用时抛出异常")
    void generate_whenRuleDisabled_shouldThrowException() {
        testRule.setStatus(0);
        when(codeRuleMapper.selectByRuleCode("ORDER_NO")).thenReturn(testRule);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> codeGeneratorService.generate("ORDER_NO")
        );
        assertEquals(ErrorCodeEnum.CODE_RULE_DISABLED.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("generate: 每日重置类型，首次生成序号为1")
    void generate_withDailyReset_firstTime_shouldStartFromOne() {
        testRule.setResetType(CodeResetTypeEnum.DAY.getCode());
        testRule.setSeqLength(6);

        // 设置序号记录的上次日期为今天，序号已重置
        testSequence.setCurrentSeq(0L);
        testSequence.setLastDate(LocalDate.now());

        when(codeRuleMapper.selectByRuleCode("ORDER_NO")).thenReturn(testRule);
        when(codeSequenceMapper.selectByRuleCode("ORDER_NO")).thenReturn(testSequence);
        when(codeSequenceMapper.updateById(any(CodeSequenceEntity.class))).thenReturn(1);

        String code = codeGeneratorService.generate("ORDER_NO");

        assertNotNull(code);
        assertTrue(code.startsWith("ORD-"));
        assertTrue(code.contains("000001") || code.contains("000002"));
    }

    @Test
    @DisplayName("generate: 序号递增")
    void generate_shouldIncrementSequence() {
        when(codeRuleMapper.selectByRuleCode("ORDER_NO")).thenReturn(testRule);
        when(codeSequenceMapper.selectByRuleCode("ORDER_NO")).thenReturn(testSequence);
        when(codeSequenceMapper.updateById(any(CodeSequenceEntity.class))).thenReturn(1);

        String code = codeGeneratorService.generate("ORDER_NO");

        assertNotNull(code);
        assertTrue(code.startsWith("ORD-"));
    }

    @Test
    @DisplayName("preview: 返回预览编码")
    void preview_shouldReturnPreviewCode() {
        when(codeRuleMapper.selectByRuleCode("ORDER_NO")).thenReturn(testRule);

        String preview = codeGeneratorService.preview("ORDER_NO");

        assertNotNull(preview);
        assertTrue(preview.startsWith("ORD-"));
    }

    @Test
    @DisplayName("preview: 规则不存在时抛出异常")
    void preview_whenRuleNotExists_shouldThrowException() {
        when(codeRuleMapper.selectByRuleCode("NOT_EXISTS")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> codeGeneratorService.preview("NOT_EXISTS")
        );
        assertEquals(ErrorCodeEnum.CODE_RULE_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("generateWithBizPrefix: 生成带业务前缀的编码")
    void generateWithBizPrefix_shouldReturnCodeWithPrefix() {
        when(codeRuleMapper.selectByRuleCode("INSTRUCTION_NO")).thenReturn(testRule);
        when(codeSequenceMapper.selectByRuleCode("INSTRUCTION_NO")).thenReturn(testSequence);
        when(codeSequenceMapper.updateById(any(CodeSequenceEntity.class))).thenReturn(1);

        String code = codeGeneratorService.generateWithBizPrefix("INSTRUCTION_NO", "20260324");

        assertNotNull(code);
        assertTrue(code.startsWith("ORD-20260324-"));
    }

    @Test
    @DisplayName("generateWithCustomPrefix: 生成带自定义前缀的编码")
    void generateWithCustomPrefix_shouldReturnCodeWithCustomPrefix() {
        // 设置 ORG_NO 规则（前缀为 NULL）
        CodeRuleEntity orgRule = new CodeRuleEntity();
        orgRule.setId(2L);
        orgRule.setRuleCode("ORG_NO");
        orgRule.setRuleName("机构编码");
        orgRule.setPrefix(null);
        orgRule.setSeqLength(4);
        orgRule.setResetType(CodeResetTypeEnum.NEVER.getCode());
        orgRule.setStep(1);
        orgRule.setStatus(1);

        CodeSequenceEntity orgSequence = new CodeSequenceEntity();
        orgSequence.setId(2L);
        orgSequence.setRuleCode("ORG_NO");
        orgSequence.setCurrentSeq(100L);
        orgSequence.setLastDate(LocalDate.now());
        orgSequence.setVersion(0);

        when(codeRuleMapper.selectByRuleCode("ORG_NO")).thenReturn(orgRule);
        when(codeSequenceMapper.selectByRuleCode("ORG_NO")).thenReturn(orgSequence);
        when(codeSequenceMapper.updateById(any(CodeSequenceEntity.class))).thenReturn(1);

        String code = codeGeneratorService.generateWithCustomPrefix("ORG_NO", "ORG-P-");

        assertNotNull(code);
        assertTrue(code.startsWith("ORG-P-"));
        assertTrue(code.endsWith("0001"));
    }

    @Test
    @DisplayName("generateWithCustomPrefix: 不同前缀生成不同编码")
    void generateWithCustomPrefix_withDifferentPrefix_shouldGenerateCorrectCode() {
        CodeRuleEntity orgRule = new CodeRuleEntity();
        orgRule.setId(2L);
        orgRule.setRuleCode("ORG_NO");
        orgRule.setRuleName("机构编码");
        orgRule.setPrefix(null);
        orgRule.setSeqLength(4);
        orgRule.setResetType(CodeResetTypeEnum.NEVER.getCode());
        orgRule.setStep(1);
        orgRule.setStatus(1);

        CodeSequenceEntity orgSequence = new CodeSequenceEntity();
        orgSequence.setId(2L);
        orgSequence.setRuleCode("ORG_NO");
        orgSequence.setCurrentSeq(5L);
        orgSequence.setLastDate(LocalDate.now());
        orgSequence.setVersion(0);

        when(codeRuleMapper.selectByRuleCode("ORG_NO")).thenReturn(orgRule);
        when(codeSequenceMapper.selectByRuleCode("ORG_NO")).thenReturn(orgSequence);
        when(codeSequenceMapper.updateById(any(CodeSequenceEntity.class))).thenReturn(1);

        // 生成生产企业编码
        String prodCode = codeGeneratorService.generateWithCustomPrefix("ORG_NO", "ORG-P-");
        assertTrue(prodCode.startsWith("ORG-P-"));

        // 生成经销商编码
        String distCode = codeGeneratorService.generateWithCustomPrefix("ORG_NO", "ORG-D-");
        assertTrue(distCode.startsWith("ORG-D-"));
    }
}
