package com.yigongbao.module.basic.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.ProductCategoryDTO;
import com.yigongbao.module.basic.product.dto.ProductListDTO;
import com.yigongbao.module.basic.product.dto.ProductPageDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.vo.ProductVO;

import java.util.List;

/**
 * 产品型号 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface ProductService extends IService<ProductEntity> {

    /**
     * 分页查询产品列表
     */
    IPage<ProductVO> listProducts(ProductPageDTO dto);

    /**
     * 查询所有产品列表
     */
    List<ProductVO> listAll(ProductListDTO dto);

    /**
     * 根据ID查询产品
     */
    ProductVO getById(Long id);

    /**
     * 创建产品
     */
    void create(CreateProductDTO dto);

    /**
     * 更新产品
     */
    void update(Long id, UpdateProductDTO dto);

    /**
     * 删除产品
     */
    void remove(Long id);

    /**
     * 按注册证查询产品
     */
    List<ProductVO> listByCertId(Long certId);

    /**
     * 按分类查询产品
     */
    List<ProductVO> listByCategory(String category);
}
