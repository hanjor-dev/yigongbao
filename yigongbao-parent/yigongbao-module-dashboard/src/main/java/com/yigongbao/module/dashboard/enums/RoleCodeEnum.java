package com.yigongbao.module.dashboard.enums;

import lombok.Getter;

/**
 * 角色代码枚举
 */
@Getter
public enum RoleCodeEnum {
    ADMIN("admin", "超级管理员"),
    SALESMAN("salesman", "业务员"),
    REGIONAL_MANAGER("regional-manager", "区域管理员"),
    DESIGNER("designer", "设计师"),
    DESIGNER_MANAGER("designer-manager", "设计管理员"),
    PRODUCTION_WORKER("production-worker", "生产员"),
    PRODUCTION_MANAGER("production-manager", "生产管理员"),
    QC("qc", "QC"),
    QC_MANAGER("qc-manager", "质检管理员"),
    WAREHOUSE_MANAGER("warehouse-manager", "库管"),
    FINANCE("finance", "财务"),
    COMPANY_ADMIN("company-admin", "公司管理员");

    private final String code;
    private final String name;

    RoleCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static RoleCodeEnum fromCode(String code) {
        for (RoleCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("不支持的角色代码: " + code);
    }
}
