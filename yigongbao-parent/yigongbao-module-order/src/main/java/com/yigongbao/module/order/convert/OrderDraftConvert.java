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
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Component
public class OrderDraftConvert {

    public static OrderDraftEntity toEntity(CreateOrderDraftDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderDraftEntity entity = new OrderDraftEntity();
        BeanUtils.copyProperties(dto, entity, "items");
        return entity;
    }

    public static List<OrderItemDraftEntity> toItemEntities(List<OrderItemDraftItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<OrderItemDraftEntity> entities = new ArrayList<>();
        for (OrderItemDraftItemDTO item : items) {
            entities.add(toItemEntity(item));
        }
        return entities;
    }

    public static OrderItemDraftEntity toItemEntity(OrderItemDraftItemDTO dto) {
        if (dto == null) {
            return null;
        }
        OrderItemDraftEntity entity = new OrderItemDraftEntity();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
