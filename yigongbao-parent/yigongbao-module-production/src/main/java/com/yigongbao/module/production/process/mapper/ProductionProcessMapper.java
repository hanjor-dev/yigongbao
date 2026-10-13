package com.yigongbao.module.production.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工序记录 Mapper
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Mapper
public interface ProductionProcessMapper extends BaseMapper<ProductionProcessEntity> {
}
