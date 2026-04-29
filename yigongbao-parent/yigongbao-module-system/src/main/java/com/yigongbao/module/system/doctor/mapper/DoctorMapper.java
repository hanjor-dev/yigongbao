package com.yigongbao.module.system.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 医生 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface DoctorMapper extends BaseMapper<DoctorEntity> {

    /**
     * 查询指定医院下的医生列表
     *
     * @param hospitalId 医院ID（sys_org.id，orgType=1.3）
     * @return 医生列表
     */
    @Select("SELECT * FROM doctor WHERE hospital_id = #{hospitalId} AND is_deleted = 0")
    List<DoctorEntity> selectByHospitalId(Long hospitalId);
}
