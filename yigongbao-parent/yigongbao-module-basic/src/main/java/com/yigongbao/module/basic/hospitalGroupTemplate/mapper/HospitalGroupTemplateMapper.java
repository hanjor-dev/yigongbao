package com.yigongbao.module.basic.hospitalGroupTemplate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 医院组合模板 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Mapper
public interface HospitalGroupTemplateMapper extends BaseMapper<HospitalGroupTemplateEntity> {

    /**
     * 根据模板编码查询ID
     */
    @Select("SELECT id FROM hospital_group_template WHERE template_code = #{code} AND is_deleted = 0 LIMIT 1")
    Long selectIdByCode(@Param("code") String code);

    /**
     * 统计模板名称是否存在
     */
    @Select("SELECT COUNT(*) FROM hospital_group_template WHERE template_name = #{name} AND is_deleted = 0")
    Long countByName(@Param("name") String name);
}
