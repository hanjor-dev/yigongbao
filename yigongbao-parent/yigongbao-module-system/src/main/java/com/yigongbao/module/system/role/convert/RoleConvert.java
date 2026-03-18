package com.yigongbao.module.system.role.convert;

import com.yigongbao.module.system.role.dto.CreateRoleDTO;
import com.yigongbao.module.system.role.dto.UpdateRoleDTO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.vo.RoleVO;
import org.springframework.beans.BeanUtils;

/**
 * 角色转换器
 * 用于 Entity/VO/DTO 之间的转换
 *
 * @author hanjor
 * @date 2026-03-17
 */
public class RoleConvert {

    /**
     * Entity 转 VO
     *
     * @param entity 角色实体
     * @return 角色视图对象
     */
    public static RoleVO toVO(RoleEntity entity) {
        if (entity == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     *
     * @param dto 创建角色DTO
     * @return 角色实体
     */
    public static RoleEntity toEntity(CreateRoleDTO dto) {
        if (dto == null) {
            return null;
        }
        RoleEntity entity = new RoleEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * DTO 转 Entity（更新）
     *
     * @param dto 更新角色DTO
     * @return 角色实体
     */
    public static RoleEntity toEntity(UpdateRoleDTO dto) {
        if (dto == null) {
            return null;
        }
        RoleEntity entity = new RoleEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
