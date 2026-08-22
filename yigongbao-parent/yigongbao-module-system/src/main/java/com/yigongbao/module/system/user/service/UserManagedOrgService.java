package com.yigongbao.module.system.user.service;

import java.util.List;

public interface UserManagedOrgService {
    List<Long> getManagedOrgIds(Long userId);
    List<Long> getEffectiveOrgIds(Long userId);
    void replaceManagedOrgIds(Long userId, Long primaryOrgId, List<Long> managedOrgIds);
}
