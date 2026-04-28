package com.yigongbao.module.system.hospitalGroupTemplate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.hospitalGroupTemplate.entity.HospitalGroupTemplateDetailEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 医院组合模板明细 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Mapper
public interface HospitalGroupTemplateDetailMapper extends BaseMapper<HospitalGroupTemplateDetailEntity> {

    @Select("SELECT COUNT(*) FROM hospital_group_template_detail WHERE template_id = #{templateId}")
    Long countByTemplateId(@Param("templateId") Long templateId);

    @Select("DELETE FROM hospital_group_template_detail WHERE template_id = #{templateId}")
    void deleteByTemplateId(@Param("templateId") Long templateId);

    @Insert("<script>" +
            "INSERT INTO hospital_group_template_detail (template_id, hospital_id, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.templateId}, #{item.hospitalId}, NOW())" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<HospitalGroupTemplateDetailEntity> list);
}
