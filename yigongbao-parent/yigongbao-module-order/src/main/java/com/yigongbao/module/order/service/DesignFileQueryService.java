package com.yigongbao.module.order.service;

import com.yigongbao.module.order.vo.order.DesignFileDetailVO;

/**
 * 订单详情所需的设计阶段文件查询契约。
 *
 * <p>由设计模块提供实现，避免订单模块反向依赖设计模块。</p>
 */
public interface DesignFileQueryService {

    /**
     * 查询订单关联的设计阶段文件。
     *
     * @param orderId 订单ID
     * @return 设计阶段文件信息
     */
    DesignFileDetailVO getDesignFiles(Long orderId);
}
