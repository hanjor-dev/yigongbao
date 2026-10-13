package com.yigongbao.module.system.config.convert;

import com.yigongbao.module.system.config.dto.CreateConfigDTO;
import com.yigongbao.module.system.config.dto.UpdateConfigDTO;
import com.yigongbao.module.system.config.entity.ConfigEntity;
import com.yigongbao.module.system.config.vo.ConfigVO;
import org.springframework.beans.BeanUtils;

/**
 * 配置 Convert
 *
 * @author hanjor
 * @date 2026-03-18
 */
public class ConfigConvert {

    /**
     * Entity 转 VO
     *
     * @param entity 实体
     * @return VO
     */
    public static ConfigVO toVO(ConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        ConfigVO vo = new ConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     *
     * @param dto DTO
     * @return 实体
     */
    public static ConfigEntity toEntity(CreateConfigDTO dto) {
        if (dto == null) {
            return null;
        }
        ConfigEntity entity = new ConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * 更新 DTO 转 Entity
     *
     * @param dto DTO
     * @param entity 实体
     */
    public static void updateEntity(UpdateConfigDTO dto, ConfigEntity entity) {
        if (dto == null || entity == null) {
            return;
        }
        BeanUtils.copyProperties(dto, entity, "id", "configKey", "isSystem");
    }
}
