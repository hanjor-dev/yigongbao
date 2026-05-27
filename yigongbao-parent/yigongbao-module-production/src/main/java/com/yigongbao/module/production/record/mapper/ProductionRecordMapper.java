package com.yigongbao.module.production.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产流转卡 Mapper
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Mapper
public interface ProductionRecordMapper extends BaseMapper<ProductionRecordEntity> {
}
