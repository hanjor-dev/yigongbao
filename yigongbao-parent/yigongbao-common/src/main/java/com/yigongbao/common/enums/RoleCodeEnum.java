package com.yigongbao.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统角色编码枚举
 * 与 sys_role.role_code 字段值保持一致
 *
 * @author hanjor
 * @date 2026-05-26
 */
@Getter
@AllArgsConstructor
public enum RoleCodeEnum {

    ADMIN("admin", "超级管理员"),
    COMPANY_ADMIN("company-admin", "公司管理员"),
    SALESMAN("salesman", "业务员"),
    SALESMAN_SELF("salesman-self", "业务员（自营）"),
    REGIONAL_MANAGER("regional-manager", "区域管理员"),
    DESIGNER("designer", "设计师"),
    DESIGNER_MANAGER("designer-manager", "设计管理员"),
    PRODUCTION_WORKER("production-worker", "生产员"),
    PRODUCTION_MANAGER("production-manager", "生产管理员"),
    QC_WORKER("qc", "质检员"),
    QC_MANAGER("qc-manager", "质检管理员"),
    WAREHOUSE_MANAGER("warehouse-manager", "库管"),
    FINANCE("finance", "财务");

    private final String code;
    private final String desc;
}
