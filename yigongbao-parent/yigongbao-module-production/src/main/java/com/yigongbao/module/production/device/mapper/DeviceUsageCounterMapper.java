package com.yigongbao.module.production.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.device.entity.DeviceUsageCounterEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备每日使用次数计数器 Mapper
 * <p>
 * 负责设备使用计数器数据的持久层操作，提供基础CRUD功能。
 * </p>
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Mapper
public interface DeviceUsageCounterMapper extends BaseMapper<DeviceUsageCounterEntity> {
}
