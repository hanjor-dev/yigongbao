package com.yigongbao.module.basic.registrationCert.convert;

import com.yigongbao.module.basic.registrationCert.dto.CreateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.entity.RegistrationCertEntity;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import org.springframework.beans.BeanUtils;

/**
 * 注册证转换器
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class RegistrationCertConvert {

    /**
     * Entity 转 VO
     */
    public static RegistrationCertVO toVO(RegistrationCertEntity entity) {
        if (entity == null) {
            return null;
        }
        RegistrationCertVO vo = new RegistrationCertVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     */
    public static RegistrationCertEntity toEntity(CreateRegistrationCertDTO dto) {
        if (dto == null) {
            return null;
        }
        RegistrationCertEntity entity = new RegistrationCertEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
