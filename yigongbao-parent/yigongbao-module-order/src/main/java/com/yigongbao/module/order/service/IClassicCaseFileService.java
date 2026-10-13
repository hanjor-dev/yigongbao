package com.yigongbao.module.order.service;

import java.util.List;

/**
 * 经典案例文件迁移服务
 */
public interface IClassicCaseFileService {

    /**
     * 收集订单所有关联文件ID
     */
    List<String> collectOrderFileIds(Long orderId);

    /**
     * 批量迁移文件到经典案例目录
     */
    void migrateFilesToClassicCase(Long orderId, String orderCode);
}
