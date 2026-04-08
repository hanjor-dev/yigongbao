package com.yigongbao.module.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单修改申请状态枚举
 * 对应 order_modify_apply.status 字段
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Getter
@AllArgsConstructor
public enum ModifyApplyStatusEnum {

    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已同意"),
    REJECTED("REJECTED", "已拒绝"),
    COMPLETED("COMPLETED", "已执行");

    /**
     * 状态编码（存储到数据库的值）
     */
    private final String code;

    /**
     * 状态名称（中文描述）
     */
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 枚举实例，未找到返回 null
     */
    public static ModifyApplyStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ModifyApplyStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
