package com.yigongbao.module.system.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class ManagedOrgScopeVO {
    private List<Long> managedOrgIds = List.of();
    private List<ManagedOrgSimpleVO> managedOrgs = List.of();
    private List<Long> effectiveOrgIds = List.of();
}
