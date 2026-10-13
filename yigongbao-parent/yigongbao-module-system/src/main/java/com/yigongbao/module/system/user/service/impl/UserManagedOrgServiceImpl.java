package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.entity.UserManagedOrgEntity;
import com.yigongbao.module.system.user.mapper.UserManagedOrgMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserManagedOrgService;
import com.yigongbao.module.system.user.vo.ManagedOrgScopeVO;
import com.yigongbao.module.system.user.vo.ManagedOrgSimpleVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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

    @Override
    public ManagedOrgScopeVO getManagedOrgScope(Long userId, Long primaryOrgId) {
        ManagedOrgScopeVO scope = new ManagedOrgScopeVO();
        if (userId == null) return scope;

        List<Long> relationOrgIds = userManagedOrgMapper.selectOrgIdsByUserId(userId);
        LinkedHashSet<Long> orderedRelationOrgIds = new LinkedHashSet<>();
        if (relationOrgIds != null) {
            relationOrgIds.stream()
                    .filter(id -> id != null)
                    .forEach(orderedRelationOrgIds::add);
        }

        LinkedHashSet<Long> candidateOrgIds = new LinkedHashSet<>();
        if (primaryOrgId != null) candidateOrgIds.add(primaryOrgId);
        candidateOrgIds.addAll(orderedRelationOrgIds);
        if (candidateOrgIds.isEmpty()) return scope;

        List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
                .select(OrgEntity::getId, OrgEntity::getOrgName, OrgEntity::getOrgType,
                        OrgEntity::getStatus, OrgEntity::getIsDeleted)
                .in(OrgEntity::getId, candidateOrgIds));
        Map<Long, OrgEntity> orgById = new HashMap<>();
        if (orgs != null) {
            orgs.stream()
                    .filter(org -> org != null && org.getId() != null)
                    .forEach(org -> orgById.put(org.getId(), org));
        }

        List<Long> managedOrgIds = new ArrayList<>();
        List<ManagedOrgSimpleVO> managedOrgs = new ArrayList<>();
        for (Long orgId : orderedRelationOrgIds) {
            if (orgId.equals(primaryOrgId)) continue;
            OrgEntity org = orgById.get(orgId);
            if (!isActiveBusinessOrg(org)) continue;

            managedOrgIds.add(orgId);
            ManagedOrgSimpleVO managedOrg = new ManagedOrgSimpleVO();
            managedOrg.setId(orgId);
            managedOrg.setOrgName(org.getOrgName() == null ? "" : org.getOrgName());
            managedOrgs.add(managedOrg);
        }
        scope.setManagedOrgIds(List.copyOf(managedOrgIds));
        scope.setManagedOrgs(List.copyOf(managedOrgs));

        if (isActiveBusinessOrg(orgById.get(primaryOrgId))) {
            List<Long> effectiveOrgIds = new ArrayList<>();
            effectiveOrgIds.add(primaryOrgId);
            effectiveOrgIds.addAll(managedOrgIds);
            scope.setEffectiveOrgIds(List.copyOf(effectiveOrgIds));
        }
        return scope;
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
