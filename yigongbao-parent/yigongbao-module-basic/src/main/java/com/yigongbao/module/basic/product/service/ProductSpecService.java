package com.yigongbao.module.basic.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.product.dto.CreateProductSpecDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductSpecDTO;
import com.yigongbao.module.basic.product.entity.ProductSpecEntity;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;

import java.util.List;

/**
 * 产品规格 Service 接口
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface ProductSpecService extends IService<ProductSpecEntity> {

    /**
     * 查询指定产品下的所有规格列表
     *
     * @param productId 产品ID
     * @return 规格列表
     */
    List<ProductSpecVO> listByProductId(Long productId);

    /**
     * 创建规格
     *
     * @param productId 产品ID
     * @param dto       创建 DTO
     */
    void create(Long productId, CreateProductSpecDTO dto);

    /**
     * 更新规格
     *
     * @param specId 规格ID
     * @param dto    更新 DTO
     */
    void update(Long specId, UpdateProductSpecDTO dto);

    /**
     * 删除规格（规格被 design_product 引用时拒绝删除）
     *
     * @param specId 规格ID
     */
    void remove(Long specId);

    /**
     * 查询产品下是否存在未删除的规格
     *
     * @param productId 产品ID
     * @return 是否存在规格
     */
    boolean existsByProductId(Long productId);
}
