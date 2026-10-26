package com.yigongbao.module.backup.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.security.entity.MachineIdentityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 机器身份 MyBatis-Plus Mapper。 */
@Mapper
public interface MachineIdentityMapper extends BaseMapper<MachineIdentityEntity> {

    @Select("SELECT * FROM backup_machine_identity WHERE machine_id = #{machineId} AND is_deleted = 0 LIMIT 1")
    MachineIdentityEntity selectByMachineId(String machineId);
}
