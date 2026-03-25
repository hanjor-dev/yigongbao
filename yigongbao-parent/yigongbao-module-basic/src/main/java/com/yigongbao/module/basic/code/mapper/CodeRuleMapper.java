package com.yigongbao.module.basic.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.code.entity.CodeRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 编码规则 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface CodeRuleMapper extends BaseMapper<CodeRuleEntity> {

    /**
     * 根据规则编码查询
     *
     * @param ruleCode 规则编码
     * @return 编码规则
     */
    @Select("SELECT * FROM sys_code_rule WHERE rule_code = #{ruleCode}")
    CodeRuleEntity selectByRuleCode(@Param("ruleCode") String ruleCode);
}
