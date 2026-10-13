package com.yigongbao.module.order.convert;

import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.yigongbao.module.order.dto.draft.OrderItemDraftItemDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单草稿 Convert
 * 提供 DTO 与 Entity 之间的转换方法
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Component
public class OrderDraftConvert {

    /**
     * 将创建草稿 DTO 转换为草稿实体
     *
     * @param dto 创建草稿请求参数
     * @return 草稿实体，不为 null
     */
    public static OrderDraftEntity toEntity(CreateOrderDraftDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderDraftEntity entity = new OrderDraftEntity();
        BeanUtils.copyProperties(dto, entity, "items");
        return entity;
    }

    /**
     * 将重建项目明细 DTO 列表转换为草稿明细实体列表
     *
     * @param items 重建项目明细 DTO 列表
     * @return 草稿明细实体列表，items 为空时返回空列表而非 null
     */
    public static List<OrderItemDraftEntity> toItemEntities(List<OrderItemDraftItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderItemDraftEntity> entities = new ArrayList<>(items.size());
        for (OrderItemDraftItemDTO item : items) {
            entities.add(toItemEntity(item));
        }
        return entities;
    }

    /**
     * 将单个重建项目明细 DTO 转换为草稿明细实体
     *
     * @param dto 重建项目明细 DTO
     * @return 草稿明细实体，不为 null
     */
    public static OrderItemDraftEntity toItemEntity(OrderItemDraftItemDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderItemDraftEntity entity = new OrderItemDraftEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
