package com.yigongbao.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
