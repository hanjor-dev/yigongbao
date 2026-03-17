package com.yigongbao.module.system.org.convert;

import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.vo.OrgVO;
import org.springframework.beans.BeanUtils;

/**
 * 机构转换器
 * 用于 Entity/VO/DTO 之间的转换
 *
 * @author hanjor
 * @date 2026-03-16
 */
public class OrgConvert {

    /**
     * Entity 转 VO
     *
     * @param entity 机构实体
     * @return 机构视图对象
     */
    public static OrgVO toVO(OrgEntity entity) {
        if (entity == null) {
            return null;
        }
        OrgVO vo = new OrgVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     *
     * @param dto 创建机构DTO
     * @return 机构实体
     */
    public static OrgEntity toEntity(CreateOrgDTO dto) {
        if (dto == null) {
            return null;
        }
        OrgEntity entity = new OrgEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * DTO 转 Entity（更新）
     *
     * @param dto 更新机构DTO
     * @return 机构实体
     */
    public static OrgEntity toEntity(UpdateOrgDTO dto) {
        if (dto == null) {
            return null;
        }
        OrgEntity entity = new OrgEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
