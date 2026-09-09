package com.yigongbao.module.basic.device.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.vo.DeviceStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceMapper extends BaseMapper<DeviceEntity> {

    @Select("""
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN state = 0 THEN 1 ELSE 0 END), 0) AS idle,
               COALESCE(SUM(CASE WHEN state <> 0 THEN 1 ELSE 0 END), 0) AS occupied
        FROM device ${ew.customSqlSegment}
        """)
    DeviceStatisticsVO selectStatistics(@Param(Constants.WRAPPER) Wrapper<DeviceEntity> wrapper);

    @Select("SELECT * FROM device WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    DeviceEntity selectByIdForUpdate(@Param("id") Long id);
}
