package com.yigongbao.module.production.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductionProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 生产产品服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionProductServiceImpl extends ServiceImpl<ProductionProductMapper, ProductionProductEntity>
        implements IProductionProductService {

    @Override
    public List<ProductionProductEntity> listByRecordId(Long recordId) {
        return list(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProductEntity::getId));
    }

    @Override
    public ProductionProductEntity getByProductNo(String productNo) {
        ProductionProductEntity product = getOne(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductNo, productNo));
        if (product == null) {
            log.warn("产品不存在: productNo={}", productNo);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long productId, String status) {
        ProductionProductEntity product = getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        String oldStatus = product.getStatus();
        product.setStatus(status);
        updateById(product);
        log.info("更新产品状态: productId={}, productNo={}, {} -> {}",
                productId, product.getProductNo(), oldStatus, status);
    }
}
