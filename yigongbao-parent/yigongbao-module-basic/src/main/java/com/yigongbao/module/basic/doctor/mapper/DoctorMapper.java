package com.yigongbao.module.basic.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.doctor.entity.DoctorEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医生 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface DoctorMapper extends BaseMapper<DoctorEntity> {
}
