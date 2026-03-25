package com.yigongbao.module.basic.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品型号 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductEntity> {
}
