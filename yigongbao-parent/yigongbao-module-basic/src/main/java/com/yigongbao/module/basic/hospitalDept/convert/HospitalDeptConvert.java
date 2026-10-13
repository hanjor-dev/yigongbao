package com.yigongbao.module.basic.hospitalDept.convert;

import com.yigongbao.module.basic.hospitalDept.dto.CreateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.dto.UpdateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.entity.HospitalDeptEntity;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
import org.springframework.beans.BeanUtils;

/**
 * 医院科室转换器
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class HospitalDeptConvert {

    /**
     * Entity 转 VO
     */
    public static HospitalDeptVO toVO(HospitalDeptEntity entity) {
        if (entity == null) {
            return null;
        }
        HospitalDeptVO vo = new HospitalDeptVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     */
    public static HospitalDeptEntity toEntity(CreateHospitalDeptDTO dto) {
        if (dto == null) {
            return null;
        }
        HospitalDeptEntity entity = new HospitalDeptEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
