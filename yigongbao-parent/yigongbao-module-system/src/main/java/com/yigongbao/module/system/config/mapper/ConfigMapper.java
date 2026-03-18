package com.yigongbao.module.system.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.config.entity.ConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置 Mapper
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Mapper
public interface ConfigMapper extends BaseMapper<ConfigEntity> {
}
