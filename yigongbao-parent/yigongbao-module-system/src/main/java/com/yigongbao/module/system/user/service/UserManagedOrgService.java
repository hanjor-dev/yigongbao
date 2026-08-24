package com.yigongbao.module.system.user.service;

import com.yigongbao.module.system.user.vo.ManagedOrgScopeVO;

import java.util.List;

public interface UserManagedOrgService {
    List<Long> getManagedOrgIds(Long userId);
    List<Long> getEffectiveOrgIds(Long userId);
    ManagedOrgScopeVO getManagedOrgScope(Long userId, Long primaryOrgId);
    void replaceManagedOrgIds(Long userId, Long primaryOrgId, List<Long> managedOrgIds);
}
