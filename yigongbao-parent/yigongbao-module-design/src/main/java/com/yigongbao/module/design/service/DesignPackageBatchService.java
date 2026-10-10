package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignPackageBatchEntity;

public interface DesignPackageBatchService extends IService<DesignPackageBatchEntity> {

    DesignPackageBatchEntity createOrReuse(Long orderId);

    /**
     * 完成追加批次，并仅为该批次的数据包触发生产数据生成。
     */
    void complete(Long orderId, Long batchId);
}
