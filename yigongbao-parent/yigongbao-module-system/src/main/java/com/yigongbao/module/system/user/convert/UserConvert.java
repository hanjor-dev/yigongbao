package com.yigongbao.module.system.user.convert;

import com.yigongbao.module.system.user.dto.CreateUserDTO;
import com.yigongbao.module.system.user.dto.UpdateUserDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.vo.UserVO;
import org.springframework.beans.BeanUtils;

/**
 * 用户对象转换器
 * 负责 UserEntity、UserVO、CreateUserDTO、UpdateUserDTO 之间的属性拷贝
 *
 * @author hanjor
 * @date 2026-03-17
 */
public class UserConvert {

    /**
     * 将用户实体转换为视图对象
     *
     * @param entity 用户实体
     * @return 用户VO，entity 为 null 时返回 null
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
     * 将创建用户 DTO 转换为实体
     *
     * @param dto 创建用户请求参数
     * @return 用户实体，dto 为 null 时返回 null
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
     * 将更新用户 DTO 转换为实体
     *
     * @param dto 更新用户请求参数
     * @return 用户实体，dto 为 null 时返回 null
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
