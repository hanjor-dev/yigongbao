package com.yigongbao.module.order.enums;

import lombok.Getter;

/**
 * 修改申请审核操作枚举
 *
 * @author hanjor
 * @date 2026-04-12
 */
@Getter
public enum AuditActionEnum {

    APPROVE("APPROVE", "同意"),
    REJECT("REJECT", "拒绝");

    private final String code;
    private final String name;

    AuditActionEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static AuditActionEnum getByCode(String code) {
        if (code == null) return null;
        for (AuditActionEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }
}
