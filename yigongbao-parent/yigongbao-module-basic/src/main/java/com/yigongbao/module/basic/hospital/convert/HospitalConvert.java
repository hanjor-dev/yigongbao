package com.yigongbao.module.basic.hospital.convert;

import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.dto.UpdateHospitalDTO;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 医院转换器
 * 实现 Entity/VO/DTO 之间的相互转换
 *
 * @author hanjor
 * @date 2026-03-19
 */
public class HospitalConvert {

    private HospitalConvert() {
    }

    /**
     * Entity 转换为 VO
     *
     * @param entity 医院实体
     * @return 医院VO
     */
    public static HospitalVO toVO(HospitalEntity entity) {
        if (entity == null) {
            return null;
        }
        HospitalVO vo = new HospitalVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * Entity 列表转换为 VO 列表
     *
     * @param entityList 医院实体列表
     * @return 医院VO列表
     */
    public static List<HospitalVO> toVOList(List<HospitalEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream()
                .map(HospitalConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * CreateDTO 转换为 Entity
     *
     * @param dto 创建参数
     * @return 医院实体
     */
    public static HospitalEntity toEntity(CreateHospitalDTO dto) {
        if (dto == null) {
            return null;
        }
        HospitalEntity entity = new HospitalEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * UpdateDTO 转换为 Entity
     *
     * @param dto 更新参数
     * @return 医院实体
     */
    public static HospitalEntity toEntity(UpdateHospitalDTO dto) {
        if (dto == null) {
            return null;
        }
        HospitalEntity entity = new HospitalEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
