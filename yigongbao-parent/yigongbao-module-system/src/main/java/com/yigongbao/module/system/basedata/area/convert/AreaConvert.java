package com.yigongbao.module.system.basedata.area.convert;

import com.yigongbao.module.system.basedata.area.entity.AreaEntity;
import com.yigongbao.module.system.basedata.area.vo.AreaVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 地区转换器
 * 实现 Entity 与 VO 之间的相互转换（与 cnarea_2023 字段对应）
 *
 * @author hanjor
 * @date 2026-03-17
 */
public class AreaConvert {

    /**
     * Entity 转换为 VO
     *
     * @param entity 地区实体
     * @return 地区VO
     */
    public static AreaVO toVO(AreaEntity entity) {
        if (entity == null) {
            return null;
        }
        AreaVO vo = new AreaVO();
        vo.setId(entity.getId());
        vo.setLevel(entity.getLevel());
        vo.setParentCode(entity.getParentCode());
        vo.setAreaCode(entity.getAreaCode());
        vo.setZipCode(entity.getZipCode());
        vo.setCityCode(entity.getCityCode());
        vo.setName(entity.getName());
        vo.setShortName(entity.getShortName());
        vo.setMergerName(entity.getMergerName());
        vo.setPinyin(entity.getPinyin());
        vo.setLng(entity.getLng());
        vo.setLat(entity.getLat());
        return vo;
    }

    /**
     * Entity 列表转换为 VO 列表
     *
     * @param entityList 地区实体列表
     * @return 地区VO列表
     */
    public static List<AreaVO> toVOList(List<AreaEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream()
                .map(AreaConvert::toVO)
                .collect(Collectors.toList());
    }
}
