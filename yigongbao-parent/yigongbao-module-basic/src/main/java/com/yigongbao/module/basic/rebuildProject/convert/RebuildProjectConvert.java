package com.yigongbao.module.basic.rebuildProject.convert;

import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.entity.RebuildProjectEntity;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 重建项目转换器
 * 实现 Entity/VO/DTO 之间的相互转换
 *
 * @author hanjor
 * @date 2026-03-23
 */
public class RebuildProjectConvert {

    private RebuildProjectConvert() {
    }

    /**
     * Entity 转换为 VO
     *
     * @param entity 项目实体
     * @return 项目VO
     */
    public static RebuildProjectVO toVO(RebuildProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        RebuildProjectVO vo = new RebuildProjectVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * Entity 列表转换为 VO 列表
     *
     * @param entityList 项目实体列表
     * @return 项目VO列表
     */
    public static List<RebuildProjectVO> toVOList(List<RebuildProjectEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream()
                .map(RebuildProjectConvert::toVO)
                .collect(Collectors.toList());
    }

    /**
     * CreateDTO 转换为 Entity
     *
     * @param dto 创建参数
     * @return 项目实体
     */
    public static RebuildProjectEntity toEntity(CreateRebuildProjectDTO dto) {
        if (dto == null) {
            return null;
        }
        RebuildProjectEntity entity = new RebuildProjectEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
