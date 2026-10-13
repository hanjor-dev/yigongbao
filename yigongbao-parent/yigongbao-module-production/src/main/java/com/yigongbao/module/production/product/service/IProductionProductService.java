package com.yigongbao.module.production.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.product.dto.ProductionProductPageDTO;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.vo.ProductionProductDetailVO;

import java.util.List;

/**
 * 生产产品服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionProductService extends IService<ProductionProductEntity> {
    List<ProductionProductEntity> listByRecordId(Long recordId);
    ProductionProductEntity getByProductNo(String productNo);
    void updateStatus(Long productId, String status);
    IPage<ProductionProductDetailVO> pageProductDetails(ProductionProductPageDTO dto);
}
