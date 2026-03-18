package com.yigongbao.module.system.user.convert;

import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.vo.UserVO;
import org.springframework.beans.BeanUtils;

/**
 * 用户转换器
 * 用于 Entity/VO/DTO 之间的转换
 *
 * @author hanjor
 * @date 2026-03-17
 */
public class UserConvert {

    /**
     * Entity 转 VO
     */
    public static UserVO toVO(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     */
    public static UserEntity toEntity(CreateUserDTO dto) {
        if (dto == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    /**
     * DTO 转 Entity（更新）
     */
    public static UserEntity toEntity(UpdateUserDTO dto) {
        if (dto == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
