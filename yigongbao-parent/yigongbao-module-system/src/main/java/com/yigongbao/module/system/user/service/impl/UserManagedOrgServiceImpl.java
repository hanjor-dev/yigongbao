package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.entity.UserManagedOrgEntity;
import com.yigongbao.module.system.user.mapper.UserManagedOrgMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserManagedOrgService;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagedOrgServiceImpl implements UserManagedOrgService {
    private final UserManagedOrgMapper userManagedOrgMapper;
    private final UserMapper userMapper;
    private final OrgMapper orgMapper;

    @Override
    public List<Long> getManagedOrgIds(Long userId) {
        if (userId == null) return List.of();
        List<Long> ids = userManagedOrgMapper.selectOrgIdsByUserId(userId);
        return ids == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(ids));
    }

    @Override
    public List<Long> getEffectiveOrgIds(Long userId) {
        if (userId == null) return List.of();
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getOrgId() == null) return List.of();
        OrgEntity primaryOrg = orgMapper.selectById(user.getOrgId());
        if (!isActiveBusinessOrg(primaryOrg)) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(user.getOrgId());
        List<Long> managedOrgIds = getManagedOrgIds(userId);
        if (!managedOrgIds.isEmpty()) {
            orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
                            .in(OrgEntity::getId, managedOrgIds)).stream()
                    .filter(this::isActiveBusinessOrg)
                    .map(OrgEntity::getId)
                    .forEach(ids::add);
        }
        return new ArrayList<>(ids);
    }

    private boolean isActiveBusinessOrg(OrgEntity org) {
        return org != null
                && Integer.valueOf(StatusConstants.NOT_DELETED).equals(org.getIsDeleted())
                && Integer.valueOf(StatusConstants.NORMAL).equals(org.getStatus())
                && (DictCodeConstants.ORG_TYPE_DEALER.equals(org.getOrgType())
                    || DictCodeConstants.ORG_TYPE_SERVICE_PROVIDER.equals(org.getOrgType()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceManagedOrgIds(Long userId, Long primaryOrgId, List<Long> managedOrgIds) {
        userManagedOrgMapper.deleteByUserId(userId);
        if (managedOrgIds == null || managedOrgIds.isEmpty()) return;
        managedOrgIds.stream()
                .filter(id -> id != null && !id.equals(primaryOrgId))
                .distinct()
                .forEach(orgId -> {
                    UserManagedOrgEntity relation = new UserManagedOrgEntity();
                    relation.setUserId(userId);
                    relation.setOrgId(orgId);
                    userManagedOrgMapper.insert(relation);
                });
    }
}
