package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignPackageBatchEntity;

import java.util.List;

public interface DesignPackageBatchService extends IService<DesignPackageBatchEntity> {

    DesignPackageBatchEntity createOrReuse(Long orderId);

    /** 获取订单当前未完成的追加批次。 */
    DesignPackageBatchEntity findUnfinished(Long orderId);

    /**
     * 在订单完成设计时收口所有未完成追加批次，并返回本次新增的数据包。
     */
    List<Long> completeUnfinishedBatches(Long orderId);
}
