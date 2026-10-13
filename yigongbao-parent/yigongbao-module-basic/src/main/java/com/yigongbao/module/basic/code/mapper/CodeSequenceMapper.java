package com.yigongbao.module.basic.code.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.code.entity.CodeSequenceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 编码序号 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface CodeSequenceMapper extends BaseMapper<CodeSequenceEntity> {

    /**
     * 根据规则编码查询序号
     *
     * @param ruleCode 规则编码
     * @return 编码序号
     */
    @Select("SELECT * FROM sys_code_sequence WHERE rule_code = #{ruleCode}")
    CodeSequenceEntity selectByRuleCode(@Param("ruleCode") String ruleCode);

    /**
     * 根据规则编码和业务标识查询序号
     *
     * @param ruleCode 规则编码
     * @param bizKey 业务标识
     * @return 编码序号
     */
    @Select("SELECT * FROM sys_code_sequence WHERE rule_code = #{ruleCode} AND biz_key = #{bizKey}")
    CodeSequenceEntity selectByRuleCodeAndBizKey(@Param("ruleCode") String ruleCode, @Param("bizKey") String bizKey);

}
