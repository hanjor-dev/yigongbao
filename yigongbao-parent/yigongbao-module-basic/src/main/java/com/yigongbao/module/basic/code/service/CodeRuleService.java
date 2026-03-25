package com.yigongbao.module.basic.code.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.code.dto.CreateCodeRuleDTO;
import com.yigongbao.module.basic.code.dto.UpdateCodeRuleDTO;
import com.yigongbao.module.basic.code.entity.CodeRuleEntity;
import com.yigongbao.module.basic.code.vo.CodeRuleVO;

/**
 * 编码规则 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface CodeRuleService extends IService<CodeRuleEntity> {

    /**
     * 分页查询编码规则
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param ruleCode 规则编码（模糊查询）
     * @param ruleName 规则名称（模糊查询）
     * @param status 状态
     * @return 分页结果
     */
    IPage<CodeRuleVO> listRules(Integer pageNum, Integer pageSize, String ruleCode, String ruleName, Integer status);

    /**
     * 根据规则编码查询
     *
     * @param ruleCode 规则编码
     * @return 编码规则
     */
    CodeRuleVO getByRuleCode(String ruleCode);

    /**
     * 创建编码规则
     *
     * @param dto 创建参数
     */
    void createRule(CreateCodeRuleDTO dto);

    /**
     * 更新编码规则
     *
     * @param id 规则ID
     * @param dto 更新参数
     */
    void updateRule(Long id, UpdateCodeRuleDTO dto);

    /**
     * 删除编码规则
     *
     * @param id 规则ID
     */
    void removeRule(Long id);
}
