package com.yigongbao.module.system.doctor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 医生 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface DoctorMapper extends BaseMapper<DoctorEntity> {

    /** 查询被逻辑删除的同医院同名医生，供唯一键冲突恢复处理。 */
    @Select("SELECT * FROM doctor WHERE doctor_name = #{doctorName} "
            + "AND hospital_id = #{hospitalId} AND is_deleted = 1 LIMIT 1")
    DoctorEntity selectDeletedByHospitalAndName(@Param("hospitalId") Long hospitalId,
                                                 @Param("doctorName") String doctorName);

    /** 物理删除被逻辑删除的医生，释放医院+姓名唯一键。 */
    @Delete("DELETE FROM doctor WHERE id = #{id} AND is_deleted = 1")
    int physicallyDeleteDeletedById(@Param("id") Long id);
}
