package com.yigongbao.module.production.pack.service;

import com.yigongbao.module.production.pack.dto.FillPackDTO;

/**
 * 包装服务接口
 *
 * @author hanjor
 * @date 2026-05-27
 */
public interface IProductionPackService {
    void fillPackInfo(Long recordId, FillPackDTO dto);
    void transferToWarehouse(Long recordId);
}
