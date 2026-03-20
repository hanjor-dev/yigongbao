package com.yigongbao.module.basic.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 医院 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Mapper
public interface HospitalMapper extends BaseMapper<HospitalEntity> {

    /**
     * 根据医院编码查询ID
     *
     * @param code 医院编码
     * @return 医院ID，不存在返回null
     */
    @Select("SELECT id FROM hospital WHERE hospital_code = #{code} AND is_deleted = 0 LIMIT 1")
    Long selectIdByCode(@Param("code") String code);

    /**
     * 统计医院名称是否存在
     *
     * @param name 医院名称
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM hospital WHERE hospital_name = #{name} AND is_deleted = 0")
    Long countByName(@Param("name") String name);
}
