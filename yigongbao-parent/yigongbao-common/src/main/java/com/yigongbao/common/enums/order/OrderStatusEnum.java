package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举（订单阶段）
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /**
     * 草稿（提交前）
     */
    DRAFT(10, "草稿"),

    /**
     * 待确认（已提交，待客服确认）
     */
    PENDING(20, "待确认"),

    /**
     * 处理中（客服确认后，开始处理）
     */
    PROCESSING(30, "处理中"),

    /**
     * 已完成
     */
    COMPLETED(40, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(50, "已取消");

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static OrderStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderStatusEnum enumItem : OrderStatusEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }
}
