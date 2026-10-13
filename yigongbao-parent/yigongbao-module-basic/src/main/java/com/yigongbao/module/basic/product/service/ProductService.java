package com.yigongbao.module.basic.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.vo.ProductVO;

import java.util.List;
import java.util.Map;

/**
 * 产品 Service 接口
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface ProductService extends IService<ProductEntity> {

    /**
     * 分页查询产品列表
     */
    IPage<ProductVO> listProducts(int pageNum, int pageSize, String productName,
                                   String category, Long certId, Integer status);

    /**
     * 查询所有产品列表
     */
    List<ProductVO> listAll(String productName, String category, Integer status);

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
     * 按注册证查询产品（已迁移至规格层，保留兼容）
     */
    List<ProductVO> listByCertId(Long certId);

    /**
     * 按分类查询产品
     */
    List<ProductVO> listByCategory(String category);

    /**
     * 查询所有 status=1 的产品（每个产品携带 status=1 的规格列表），供打印信息选项接口使用
     *
     * @return 产品列表（含 specs 字段，specs 为 status=1 的规格，无规格时为空列表）
     */
    List<ProductVO> listAllWithSpecs();
}
