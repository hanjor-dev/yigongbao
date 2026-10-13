package com.yigongbao.module.basic.bodyPart.convert;

import com.yigongbao.module.basic.bodyPart.dto.CreateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.entity.BodyPartEntity;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 重建部位转换器
 * 实现 Entity/VO/DTO 之间的相互转换
 *
 * @author hanjor
 * @date 2026-03-23
 */
public class BodyPartConvert {

    private BodyPartConvert() {
    }

    /**
     * Entity 转换为 VO
     *
     * @param entity 部位实体
     * @return 部位VO
     */
    public static BodyPartVO toVO(BodyPartEntity entity) {
        if (entity == null) {
            return null;
        }
        BodyPartVO vo = new BodyPartVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * Entity 列表转换为 VO 列表
     *
     * @param entityList 部位实体列表
     * @return 部位VO列表
     */
    public static List<BodyPartVO> toVOList(List<BodyPartEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream()
                .map(BodyPartConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * CreateDTO 转换为 Entity
     *
     * @param dto 创建参数
     * @return 部位实体
     */
    public static BodyPartEntity toEntity(CreateBodyPartDTO dto) {
        if (dto == null) {
            return null;
        }
        BodyPartEntity entity = new BodyPartEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
