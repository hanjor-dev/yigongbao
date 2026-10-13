package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserManagedOrgMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.vo.ManagedOrgScopeVO;
import com.yigongbao.module.system.user.vo.ManagedOrgSimpleVO;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.org.entity.OrgEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagedOrgServiceImplTest {

    @BeforeAll
    static void initializeMybatisTableInfo() {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrgEntity.class);
    }

    @Mock
    private UserManagedOrgMapper userManagedOrgMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrgMapper orgMapper;

    @InjectMocks
    private UserManagedOrgServiceImpl service;

    @Test
    @SuppressWarnings("unchecked")
    void managedOrgScope_filtersAndRebuildsSnapshotInRelationshipOrderWithOneBatchQuery() {
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L))
                .thenReturn(List.of(30L, 10L, 20L, 40L, 50L, 60L, 70L));
        when(orgMapper.selectList(any())).thenReturn(List.of(
                businessOrg(50L, "错误类型", "1.3", 1, 0),
                businessOrg(20L, "机构20", "1.4", 1, 0),
                businessOrg(10L, "主机构", "1.2", 1, 0),
                businessOrg(60L, "已删除", "1.2", 1, 1),
                businessOrg(30L, null, "1.2", 1, 0),
                businessOrg(40L, "已停用", "1.2", 0, 0)));

        ManagedOrgScopeVO scope = service.getManagedOrgScope(7L, 10L);

        assertThat(scope.getManagedOrgIds()).containsExactly(30L, 20L);
        assertThat(scope.getManagedOrgs())
                .extracting(ManagedOrgSimpleVO::getId, ManagedOrgSimpleVO::getOrgName)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(30L, ""),
                        org.assertj.core.groups.Tuple.tuple(20L, "机构20"));
        assertThat(scope.getEffectiveOrgIds()).containsExactly(10L, 30L, 20L);

        verify(userManagedOrgMapper).selectOrgIdsByUserId(7L);
        ArgumentCaptor<Wrapper<OrgEntity>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orgMapper).selectList(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isInstanceOf(LambdaQueryWrapper.class);
        LambdaQueryWrapper<OrgEntity> lambdaQuery = (LambdaQueryWrapper<OrgEntity>) queryCaptor.getValue();
        assertThat(lambdaQuery.getSqlSelect())
                .isEqualTo("id,org_name,org_type,status,is_deleted");
        AbstractWrapper<?, ?, ?> query = lambdaQuery;
        query.getSqlSegment();
        assertThat(query.getParamNameValuePairs().values())
                .containsExactlyInAnyOrder(10L, 30L, 20L, 40L, 50L, 60L, 70L);
        verify(orgMapper, never()).selectById(any());
        verifyNoInteractions(userMapper);
    }

    @Test
    void managedOrgScope_preservesEmptyAndWhitespaceNames() {
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of(20L, 30L, 40L));
        when(orgMapper.selectList(any())).thenReturn(List.of(
                businessOrg(10L, "主机构", "1.2", 1, 0),
                businessOrg(40L, "   ", "1.2", 1, 0),
                businessOrg(20L, null, "1.2", 1, 0),
                businessOrg(30L, "", "1.4", 1, 0)));

        ManagedOrgScopeVO scope = service.getManagedOrgScope(7L, 10L);

        assertThat(scope.getManagedOrgs())
                .extracting(ManagedOrgSimpleVO::getOrgName)
                .containsExactly("", "", "   ");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPrimaryOrgs")
    void managedOrgScope_invalidPrimaryStillReturnsManagedOrgsButBlocksEffectiveScope(
            String scenario, OrgEntity invalidPrimary) {
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of(20L));
        when(orgMapper.selectList(any())).thenReturn(List.of(
                businessOrg(20L, "有效额外机构", "1.4", 1, 0), invalidPrimary));

        ManagedOrgScopeVO scope = service.getManagedOrgScope(7L, 10L);

        assertThat(scope.getManagedOrgIds()).containsExactly(20L);
        assertThat(scope.getManagedOrgs())
                .extracting(ManagedOrgSimpleVO::getId, ManagedOrgSimpleVO::getOrgName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(20L, "有效额外机构"));
        assertThat(scope.getEffectiveOrgIds()).isEmpty();
        verify(userManagedOrgMapper).selectOrgIdsByUserId(7L);
        verify(orgMapper).selectList(any());
        verify(orgMapper, never()).selectById(any());
        verifyNoInteractions(userMapper);
    }

    static Stream<Arguments> invalidPrimaryOrgs() {
        return Stream.of(
                Arguments.of("主机构停用", businessOrgStatic(10L, "主机构", "1.2", 0, 0)),
                Arguments.of("主机构已删除", businessOrgStatic(10L, "主机构", "1.2", 1, 1)),
                Arguments.of("主机构类型错误", businessOrgStatic(10L, "主机构", "1.3", 1, 0)));
    }

    @Test
    void managedOrgScope_withNoAdditionalOrgsContainsOnlyValidPrimaryScope() {
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of());
        when(orgMapper.selectList(any()))
                .thenReturn(List.of(businessOrg(10L, "主机构", "1.2", 1, 0)));

        ManagedOrgScopeVO scope = service.getManagedOrgScope(7L, 10L);

        assertThat(scope.getManagedOrgIds()).isEmpty();
        assertThat(scope.getManagedOrgs()).isEmpty();
        assertThat(scope.getEffectiveOrgIds()).containsExactly(10L);
        verify(userManagedOrgMapper).selectOrgIdsByUserId(7L);
        verify(orgMapper).selectList(any());
        verify(orgMapper, never()).selectById(any());
        verifyNoInteractions(userMapper);
    }

    @Test
    void managedOrgSimpleVO_supportsJavaSerializationRoundTrip() throws Exception {
        ManagedOrgSimpleVO original = new ManagedOrgSimpleVO();
        original.setId(20L);
        original.setOrgName("华东经销商");

        byte[] serialized;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
            serialized = bytes.toByteArray();
        }

        ManagedOrgSimpleVO restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            restored = (ManagedOrgSimpleVO) input.readObject();
        }

        assertThat(restored.getId()).isEqualTo(20L);
        assertThat(restored.getOrgName()).isEqualTo("华东经销商");
    }

    @Test
    void managedOrgScopeVO_defaultsAllListsToEmpty() {
        ManagedOrgScopeVO scope = new ManagedOrgScopeVO();

        assertThat(scope.getManagedOrgIds()).isEmpty();
        assertThat(scope.getManagedOrgs()).isEmpty();
        assertThat(scope.getEffectiveOrgIds()).isEmpty();
    }

    @Test
    void managedOrgScope_returnsImmutableNonEmptySnapshotLists() {
        when(userManagedOrgMapper.selectOrgIdsByUserId(7L)).thenReturn(List.of(20L));
        when(orgMapper.selectList(any())).thenReturn(List.of(
                businessOrg(10L, "主机构", "1.2", 1, 0),
                businessOrg(20L, "额外机构", "1.4", 1, 0)));

        ManagedOrgScopeVO scope = service.getManagedOrgScope(7L, 10L);

        assertThatThrownBy(scope.getManagedOrgIds()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(scope.getManagedOrgs()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(scope.getEffectiveOrgIds()::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

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
        return businessOrg(id, null, orgType, status, isDeleted);
    }

    private OrgEntity businessOrg(Long id, String orgName, String orgType, Integer status, Integer isDeleted) {
        return businessOrgStatic(id, orgName, orgType, status, isDeleted);
    }

    private static OrgEntity businessOrgStatic(
            Long id, String orgName, String orgType, Integer status, Integer isDeleted) {
        OrgEntity org = new OrgEntity();
        org.setId(id);
        org.setOrgName(orgName);
        org.setOrgType(orgType);
        org.setStatus(status);
        org.setIsDeleted(isDeleted);
        return org;
    }
}
