package com.yigongbao.module.production.warehouse.service;

import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;

/**
 * 仓储管理服务接口
 *
 * @author hanjor
 * @date 2026-06-11
 */
public interface IWarehouseService {
    /**
     * 产品入库
     */
    void warehouseInProduct(Long productId, WarehouseInProductDTO dto);

    /**
     * 产品出库
     */
    void warehouseOutProduct(Long productId, WarehouseOutProductDTO dto);
}
