package com.yigongbao.module.system.resource.convert;

import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ResourceTypeEnum;
import com.yigongbao.module.system.resource.dto.CreateResourceDTO;
import com.yigongbao.module.system.resource.dto.UpdateResourceDTO;
import com.yigongbao.module.system.resource.entity.ResourceEntity;
import com.yigongbao.module.system.resource.vo.ResourceVO;
import org.springframework.beans.BeanUtils;

/**
 * 资源转换器
 *
 * @author hanjor
 * @date 2026-03-19
 */
public class ResourceConvert {

    /**
     * Entity 转 VO
     *
     * @param entity 资源实体
     * @return 资源VO
     */
    public static ResourceVO toVO(ResourceEntity entity) {
        if (entity == null) {
            return null;
        }
        ResourceVO vo = new ResourceVO();
        BeanUtils.copyProperties(entity, vo);

        // 设置资源类型名称
        ResourceTypeEnum typeEnum = ResourceTypeEnum.getByCode(entity.getResourceType());
        vo.setResourceTypeName(typeEnum != null ? typeEnum.getDesc() : "");

        // 设置显示状态名称
        vo.setVisibleName(StatusConstants.getStatusName(entity.getVisible()));

        // 设置状态名称
        vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));

        return vo;
    }

    /**
     * DTO 转 Entity
     *
     * @param dto 创建资源DTO
     * @return 资源实体
     */
    public static ResourceEntity toEntity(CreateResourceDTO dto) {
        if (dto == null) {
            return null;
        }
        ResourceEntity entity = new ResourceEntity();
        BeanUtils.copyProperties(dto, entity);

        // 设置默认值
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        if (entity.getVisible() == null) {
            entity.setVisible(StatusConstants.NORMAL);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(StatusConstants.NORMAL);
        }

        return entity;
    }

    /**
     * DTO 转 Entity（更新）
     *
     * @param dto 更新资源DTO
     * @return 资源实体
     */
    public static ResourceEntity toEntityForUpdate(UpdateResourceDTO dto) {
        if (dto == null) {
            return null;
        }
        ResourceEntity entity = new ResourceEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
