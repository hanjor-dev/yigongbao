package com.yigongbao.module.basic.doctor.convert;

import com.yigongbao.module.basic.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.basic.doctor.entity.DoctorEntity;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;
import org.springframework.beans.BeanUtils;

/**
 * 医生转换器
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class DoctorConvert {

    /**
     * Entity 转 VO
     */
    public static DoctorVO toVO(DoctorEntity entity) {
        if (entity == null) {
            return null;
        }
        DoctorVO vo = new DoctorVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     */
    public static DoctorEntity toEntity(CreateDoctorDTO dto) {
        if (dto == null) {
            return null;
        }
        DoctorEntity entity = new DoctorEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
