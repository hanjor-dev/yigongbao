package com.yigongbao.module.basic.code.service;

import com.yigongbao.common.exception.BusinessException;

/**
 * 编码生成服务
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface CodeGeneratorService {

    /**
     * 生成编码
     *
     * @param ruleCode 规则编码（如 ORDER_NO）
     * @return 编码字符串
     * @throws BusinessException 规则不存在或已禁用
     */
    String generate(String ruleCode);

    /**
     * 生成带业务前缀的编码
     * 用于指令单编号：ZL-{订单编号}-{序号}
     *
     * @param ruleCode 规则编码
     * @param bizPrefix 业务前缀（如订单编号）
     * @return 编码字符串
     */
    String generateWithBizPrefix(String ruleCode, String bizPrefix);

    /**
     * 生成带自定义前缀的编码
     * 用于机构编码等需要动态前缀的场景
     *
     * @param ruleCode 规则编码
     * @param prefix 自定义前缀（如 ORG-P-）
     * @return 完整编码（如 ORG-P-0001）
     */
    String generateWithCustomPrefix(String ruleCode, String prefix);

    /**
     * 生成带序号后缀的编码
     * 用于同一业务前缀下生成多个子编码，如数据包编码：202603250001-1、202603250001-2
     *
     * @param ruleCode 规则编码
     * @param bizKey 业务标识（如订单编号，用于隔离序号池）
     * @return 带序号后缀的编码（如 202603250001-1）
     */
    String generateWithSeqSuffix(String ruleCode, String bizKey);

    /**
     * 预览编码格式
     *
     * @param ruleCode 规则编码
     * @return 预览编码（示例）
     */
    String preview(String ruleCode);
}
