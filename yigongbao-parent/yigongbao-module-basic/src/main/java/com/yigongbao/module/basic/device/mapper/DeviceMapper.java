package com.yigongbao.module.basic.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceMapper extends BaseMapper<DeviceEntity> {

    @Select("SELECT * FROM device WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    DeviceEntity selectByIdForUpdate(@Param("id") Long id);
}
