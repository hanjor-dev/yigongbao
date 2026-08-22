package com.yigongbao.module.system.user.service.impl;

import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserManagedOrgMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.org.entity.OrgEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagedOrgServiceImplTest {

    @Mock
    private UserManagedOrgMapper userManagedOrgMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrgMapper orgMapper;

    @InjectMocks
    private UserManagedOrgServiceImpl service;

    private void mockActiveBusinessOrg(Long id) {
        OrgEntity org = new OrgEntity();
        org.setId(id);
        org.setOrgType("1.2");
        org.setStatus(1);
        org.setIsDeleted(0);
        when(orgMapper.selectById(id)).thenReturn(org);
    }

    @Test
    void effectiveOrgIds_containsPrimaryAndAdditionalManagedOrgs() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        when(userMapper.selectById(7L)).thenReturn(user);
        mockActiveBusinessOrg(10L);
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of(20L, 30L));
        when(orgMapper.selectList(any())).thenReturn(List.of(
                businessOrg(20L, "1.2", 1, 0),
                businessOrg(30L, "1.4", 1, 0)));

        assertThat(service.getEffectiveOrgIds(7L)).containsExactly(10L, 20L, 30L);
    }

    @Test
    void effectiveOrgIds_withNoAdditionalOrgs_containsPrimaryOnly() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        when(userMapper.selectById(7L)).thenReturn(user);
        mockActiveBusinessOrg(10L);
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of());

        assertThat(service.getEffectiveOrgIds(7L)).containsExactly(10L);
    }

    @Test
    void effectiveOrgIds_deduplicatesPrimaryFromLegacyOrInvalidRelation() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        when(userMapper.selectById(7L)).thenReturn(user);
        mockActiveBusinessOrg(10L);
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of(10L, 20L, 20L));
        when(orgMapper.selectList(any())).thenReturn(List.of(
                businessOrg(10L, "1.2", 1, 0),
                businessOrg(20L, "1.2", 1, 0)));

        assertThat(service.getEffectiveOrgIds(7L)).containsExactly(10L, 20L);
    }

    @Test
    void effectiveOrgIds_filtersDisabledDeletedAndUnsupportedManagedOrgsAtReadTime() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        when(userMapper.selectById(7L)).thenReturn(user);
        mockActiveBusinessOrg(10L);
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of(20L, 30L, 40L, 50L));

        OrgEntity activeDealer = businessOrg(20L, "1.2", 1, 0);
        OrgEntity disabledServiceProvider = businessOrg(30L, "1.4", 0, 0);
        OrgEntity deletedDealer = businessOrg(40L, "1.2", 1, 1);
        OrgEntity hospital = businessOrg(50L, "1.3", 1, 0);
        when(orgMapper.selectList(any()))
                .thenReturn(List.of(activeDealer, disabledServiceProvider, deletedDealer, hospital));

        assertThat(service.getEffectiveOrgIds(7L)).containsExactly(10L, 20L);
    }

    @Test
    void effectiveOrgIds_whenPrimaryOrgDisabled_blocksAllAccess() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        OrgEntity disabled = new OrgEntity();
        disabled.setId(10L);
        disabled.setOrgType("1.2");
        disabled.setStatus(0);
        disabled.setIsDeleted(0);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(orgMapper.selectById(10L)).thenReturn(disabled);

        assertThat(service.getEffectiveOrgIds(7L)).isEmpty();
    }

    @Test
    void effectiveOrgIds_whenPrimaryOrgDeleted_blocksAllAccess() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        OrgEntity deleted = new OrgEntity();
        deleted.setId(10L);
        deleted.setOrgType("1.2");
        deleted.setStatus(1);
        deleted.setIsDeleted(1);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(orgMapper.selectById(10L)).thenReturn(deleted);

        assertThat(service.getEffectiveOrgIds(7L)).isEmpty();
    }

    @Test
    void effectiveOrgIds_whenPrimaryOrgIsHospital_blocksAllAccess() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setOrgId(10L);
        OrgEntity hospital = new OrgEntity();
        hospital.setId(10L);
        hospital.setOrgType("1.3");
        hospital.setStatus(1);
        hospital.setIsDeleted(0);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(orgMapper.selectById(10L)).thenReturn(hospital);

        assertThat(service.getEffectiveOrgIds(7L)).isEmpty();
    }

    private OrgEntity businessOrg(Long id, String orgType, Integer status, Integer isDeleted) {
        OrgEntity org = new OrgEntity();
        org.setId(id);
        org.setOrgType(orgType);
        org.setStatus(status);
        org.setIsDeleted(isDeleted);
        return org;
    }
}
