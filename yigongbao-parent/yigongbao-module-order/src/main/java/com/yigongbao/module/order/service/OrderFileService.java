package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.entity.OrderFileEntity;

import java.util.List;

/**
 * 订单文件关联服务接口
 *
 * @author hanjor
 * @date 2026-04-20
 */
public interface OrderFileService extends IService<OrderFileEntity> {

    /**
     * 查询订单下指定分类的文件列表
     *
     * @param orderId      订单ID
     * @param fileCategory 文件分类字典编码（如 DictCodeConstants.ORDER_FILE_CATEGORY_DCM）
     * @return 文件关联记录列表，按 id 升序
     */
    List<OrderFileEntity> listByOrderIdAndCategory(Long orderId, String fileCategory);
}
