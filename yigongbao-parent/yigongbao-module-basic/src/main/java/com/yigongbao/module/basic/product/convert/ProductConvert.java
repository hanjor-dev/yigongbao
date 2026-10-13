package com.yigongbao.module.basic.product.convert;

import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.vo.ProductVO;
import org.springframework.beans.BeanUtils;

/**
 * 产品转换器
 *
 * @author hanjor
 * @date 2026-03-24
 */
public class ProductConvert {

    /**
     * Entity 转 VO
     */
    public static ProductVO toVO(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO 转 Entity
     */
    public static ProductEntity toEntity(CreateProductDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductEntity entity = new ProductEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
