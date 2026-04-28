package com.yigongbao.module.system.doctor.convert;

import com.yigongbao.module.system.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import com.yigongbao.module.system.doctor.vo.DoctorVO;
import org.springframework.beans.BeanUtils;

/**
 * 医生转换器
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class DoctorConvert {

    public static DoctorVO toVO(DoctorEntity entity) {
        if (entity == null) {
            return null;
        }
        DoctorVO vo = new DoctorVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public static DoctorEntity toEntity(CreateDoctorDTO dto) {
        if (dto == null) {
            return null;
        }
        DoctorEntity entity = new DoctorEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
