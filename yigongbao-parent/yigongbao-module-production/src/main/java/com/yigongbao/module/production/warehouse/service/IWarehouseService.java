package com.yigongbao.module.production.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseDTO;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;
import com.yigongbao.module.production.warehouse.vo.WarehouseDetailVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseProductVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseRecordVO;

/**
 * 仓储管理服务接口
 *
 * @author hanjor
 * @date 2026-06-11
 */
public interface IWarehouseService {
    IPage<WarehouseRecordVO> listWarehouse(ListWarehouseDTO dto);

    WarehouseDetailVO getWarehouseDetail(Long recordId);

    IPage<WarehouseProductVO> listWarehouseProducts(ListWarehouseProductDTO dto);

    void warehouseInProduct(Long productId, WarehouseInProductDTO dto);

    void warehouseOutProduct(Long productId, WarehouseOutProductDTO dto);
}
