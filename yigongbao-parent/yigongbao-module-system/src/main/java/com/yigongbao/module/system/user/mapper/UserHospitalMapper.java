package com.yigongbao.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.user.entity.UserHospitalEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-医院关联 Mapper
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Mapper
public interface UserHospitalMapper extends BaseMapper<UserHospitalEntity> {

    @Select("SELECT hospital_id FROM sys_user_hospital WHERE user_id = #{userId}")
    List<Long> selectHospitalIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_hospital WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user_hospital WHERE user_id = #{userId} AND hospital_id = #{hospitalId}")
    Long countByUserIdAndHospitalId(@Param("userId") Long userId, @Param("hospitalId") Long hospitalId);
}
