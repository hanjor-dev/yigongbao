package com.yigongbao.module.basic.hospitalGroupTemplate.convert;

import com.yigongbao.module.basic.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import org.springframework.beans.BeanUtils;

/**
 * 医院组合模板转换器
 *
 * @author hanjor
 * @date 2026-03-19
 */
public class HospitalGroupTemplateConvert {

    private HospitalGroupTemplateConvert() {
    }

    public static HospitalGroupTemplateEntity toEntity(CreateHospitalGroupTemplateDTO dto) {
        if (dto == null) {
            return null;
        }
        HospitalGroupTemplateEntity entity = new HospitalGroupTemplateEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    public static HospitalGroupTemplateEntity toEntity(UpdateHospitalGroupTemplateDTO dto) {
        if (dto == null) {
            return null;
        }
        HospitalGroupTemplateEntity entity = new HospitalGroupTemplateEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
