package com.yigongbao.module.basic.hospitalGroupTemplate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateDetailEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 医院组合模板明细 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Mapper
public interface HospitalGroupTemplateDetailMapper extends BaseMapper<HospitalGroupTemplateDetailEntity> {

    /**
     * 统计模板下的医院数量
     */
    @Select("SELECT COUNT(*) FROM hospital_group_template_detail WHERE template_id = #{templateId}")
    Long countByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID删除所有明细
     */
    @Delete("DELETE FROM hospital_group_template_detail WHERE template_id = #{templateId}")
    void deleteByTemplateId(@Param("templateId") Long templateId);
}
