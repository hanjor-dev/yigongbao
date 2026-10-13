package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.service.OrderFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单文件关联服务实现
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Slf4j
@Service
public class OrderFileServiceImpl extends ServiceImpl<OrderFileMapper, OrderFileEntity> implements OrderFileService {

    /**
     * 查询订单下指定分类的文件列表，按 id 升序排列
     *
     * @param orderId      订单ID
     * @param fileCategory 文件分类字典编码
     * @return 文件关联记录列表
     */
    @Override
    public List<OrderFileEntity> listByOrderIdAndCategory(Long orderId, String fileCategory) {
        return list(new LambdaQueryWrapper<OrderFileEntity>()
                .eq(OrderFileEntity::getOrderId, orderId)
                .eq(OrderFileEntity::getFileCategory, fileCategory)
                .orderByAsc(OrderFileEntity::getId));
    }
}
