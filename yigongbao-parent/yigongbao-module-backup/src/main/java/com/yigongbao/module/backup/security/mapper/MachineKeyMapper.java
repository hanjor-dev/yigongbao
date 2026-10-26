package com.yigongbao.module.backup.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.security.entity.MachineKeyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 机器密钥元数据 MyBatis-Plus Mapper。 */
@Mapper
public interface MachineKeyMapper extends BaseMapper<MachineKeyEntity> {

    @Select("SELECT * FROM backup_machine_key WHERE key_id = #{keyId} AND is_deleted = 0 LIMIT 1")
    MachineKeyEntity selectByKeyId(String keyId);
}
