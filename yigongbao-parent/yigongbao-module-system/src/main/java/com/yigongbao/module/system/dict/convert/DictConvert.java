package com.yigongbao.module.system.dict.convert;

import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.vo.DictVO;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典 Convert
 * Entity/VO/DTO 之间的转换
 *
 * @author hanjor
 * @date 2026-03-16
 */
public class DictConvert {

    /**
     * Entity 转 VO
     *
     * @param entity 字典实体
     * @return 字典VO
     */
    public static DictVO toVO(DictEntity entity) {
        if (entity == null) {
            return null;
        }
        DictVO vo = new DictVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * Entity 列表转 VO 列表
     *
     * @param entityList 字典实体列表
     * @return 字典VO列表
     */
    public static List<DictVO> toVOList(List<DictEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return new ArrayList<>();
        }
        return entityList.stream()
                .map(DictConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * VO 转 Entity
     *
     * @param vo 字典VO
     * @return 字典实体
     */
    public static DictEntity toEntity(DictVO vo) {
        if (vo == null) {
            return null;
        }
        DictEntity entity = new DictEntity();
        BeanUtils.copyProperties(vo, entity);
        return entity;
    }
}
