package com.yigongbao.module.backup.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.security.entity.MachineNonceEntity;
import org.apache.ibatis.annotations.Mapper;

/** nonce MyBatis-Plus Mapper；唯一约束由数据库保证原子防重放。 */
@Mapper
public interface MachineNonceMapper extends BaseMapper<MachineNonceEntity> {
}
