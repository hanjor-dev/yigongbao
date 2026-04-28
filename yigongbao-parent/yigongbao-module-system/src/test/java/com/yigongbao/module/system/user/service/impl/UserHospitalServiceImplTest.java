package com.yigongbao.module.system.user.service.impl;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.org.vo.OrgVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.entity.UserHospitalEntity;
import com.yigongbao.module.system.user.mapper.UserHospitalMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User-Hospital Association Service Unit Test
 *
 * @author hanjor
 * @date 2026-03-20
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserHospitalService Unit Test")
class UserHospitalServiceImplTest {

    @Mock
    private UserHospitalMapper userHospitalMapper;

    @Mock
    private OrgMapper orgMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserHospitalServiceImpl userHospitalService;

    private UserEntity testUser;
    private OrgEntity enabledHospital;
    private OrgEntity disabledHospital;
    private RoleEntity enabledRole;
    private RoleEntity disabledRole;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRoleId(1L);

        enabledHospital = new OrgEntity();
        enabledHospital.setId(1L);
        enabledHospital.setOrgName("Test Hospital 1");
        enabledHospital.setStatus(1);

        disabledHospital = new OrgEntity();
        disabledHospital.setId(2L);
        disabledHospital.setOrgName("Test Hospital 2");
        disabledHospital.setStatus(0);

        enabledRole = new RoleEntity();
        enabledRole.setId(1L);
        enabledRole.setDataScopeType("hospitals");

        disabledRole = new RoleEntity();
        disabledRole.setId(2L);
        disabledRole.setDataScopeType("org");
    }

    // ==================== getHospitalIdsByUserId Tests ====================

    @Test
    @DisplayName("getHospitalIdsByUserId: Returns IDs when user has hospitals")
    void getHospitalIdsByUserId_whenHasHospitals_shouldReturnIds() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(ids);

        List<Long> result = userHospitalService.getHospitalIdsByUserId(1L);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    @Test
    @DisplayName("getHospitalIdsByUserId: Returns empty list when no hospitals")
    void getHospitalIdsByUserId_whenNoHospitals_shouldReturnEmptyList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(List.of());

        List<Long> result = userHospitalService.getHospitalIdsByUserId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getHospitalIdsByUserId: Returns empty list when user not exists")
    void getHospitalIdsByUserId_whenUserNotExists_shouldReturnEmptyList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(999L)).thenReturn(null);

        List<Long> result = userHospitalService.getHospitalIdsByUserId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getHospitalsByUserId Tests ====================

    @Test
    @DisplayName("getHospitalsByUserId: Returns list when user has hospitals")
    void getHospitalsByUserId_whenHasHospitals_shouldReturnList() {
        List<Long> ids = List.of(1L);
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(ids);
        List<OrgEntity> hospitals = List.of(enabledHospital);
        when(orgMapper.selectBatchIds(ids)).thenReturn(hospitals);

        List<OrgVO> result = userHospitalService.getHospitalsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Hospital 1", result.get(0).getOrgName());
    }

    @Test
    @DisplayName("getHospitalsByUserId: Returns empty list when no hospitals")
    void getHospitalsByUserId_whenNoHospitals_shouldReturnEmptyList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(List.of());

        List<OrgVO> result = userHospitalService.getHospitalsByUserId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getHospitalsByUserId: Returns empty list when user not exists")
    void getHospitalsByUserId_whenUserNotExists_shouldReturnEmptyList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(999L)).thenReturn(null);

        List<OrgVO> result = userHospitalService.getHospitalsByUserId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== assignHospitals Tests ====================

    @Test
    @DisplayName("assignHospitals: Assigns hospitals successfully")
    void assignHospitals_shouldAssignHospitals() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        List<Long> ids = List.of(1L);
        when(orgMapper.selectBatchIds(ids)).thenReturn(List.of(enabledHospital));
        doNothing().when(userHospitalMapper).deleteByUserId(1L);

        userHospitalService.assignHospitals(1L, ids);

        verify(userMapper, times(1)).selectById(1L);
        verify(orgMapper, times(1)).selectBatchIds(ids);
        verify(userHospitalMapper, times(1)).deleteByUserId(1L);
        verify(userHospitalMapper, times(1)).insert(any(UserHospitalEntity.class));
    }

    @Test
    @DisplayName("assignHospitals: Throws exception when user not exists")
    void assignHospitals_whenUserNotExists_shouldThrowException() {
        when(userMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userHospitalService.assignHospitals(999L, List.of(1L)));

        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
        verify(userHospitalMapper, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("assignHospitals: Throws exception when some hospital IDs are invalid")
    void assignHospitals_whenInvalidHospitalId_shouldThrowException() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        List<Long> idsWithInvalid = List.of(1L, 2L);
        when(orgMapper.selectBatchIds(idsWithInvalid)).thenReturn(List.of(enabledHospital));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userHospitalService.assignHospitals(1L, idsWithInvalid));

        assertEquals(ErrorCodeEnum.HOSPITAL_NOT_FOUND.getCode(), exception.getCode());
        verify(userHospitalMapper, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("assignHospitals: Throws exception when hospital is disabled")
    void assignHospitals_whenHospitalDisabled_shouldThrowException() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        List<Long> ids = List.of(2L);
        when(orgMapper.selectBatchIds(ids)).thenReturn(List.of(disabledHospital));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userHospitalService.assignHospitals(1L, ids));

        assertEquals(ErrorCodeEnum.HOSPITAL_DISABLED.getCode(), exception.getCode());
        assertNotNull(exception.getMessage());
        verify(userHospitalMapper, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("assignHospitals: Only deletes old associations when hospitalIds is empty")
    void assignHospitals_whenEmpty_shouldOnlyDelete() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        doNothing().when(userHospitalMapper).deleteByUserId(1L);

        userHospitalService.assignHospitals(1L, List.of());

        verify(userHospitalMapper, times(1)).deleteByUserId(1L);
        verify(userHospitalMapper, never()).insert(any(UserHospitalEntity.class));
    }

    @Test
    @DisplayName("assignHospitals: Only deletes old associations when hospitalIds is null")
    void assignHospitals_whenNull_shouldOnlyDelete() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        doNothing().when(userHospitalMapper).deleteByUserId(1L);

        userHospitalService.assignHospitals(1L, null);

        verify(userHospitalMapper, times(1)).deleteByUserId(1L);
        verify(userHospitalMapper, never()).insert(any(UserHospitalEntity.class));
    }

    // ==================== getHospitalOptionsByUserId Tests ====================

    @Test
    @DisplayName("getHospitalOptionsByUserId: Returns all enabled hospitals")
    void getHospitalOptionsByUserId_shouldReturnAllEnabledHospitals() {
        OrgEntity hospital2 = new OrgEntity();
        hospital2.setId(2L);
        hospital2.setOrgName("Test Hospital 2");
        hospital2.setStatus(1);

        List<OrgEntity> allHospitals = Arrays.asList(enabledHospital, hospital2);
        when(orgMapper.selectList(any())).thenReturn(allHospitals);

        List<OrgVO> result = userHospitalService.getHospitalOptionsByUserId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getHospitalOptionsByUserId: Returns empty list when no enabled hospitals")
    void getHospitalOptionsByUserId_whenNoEnabledHospitals_shouldReturnEmptyList() {
        when(orgMapper.selectList(any())).thenReturn(List.of());

        List<OrgVO> result = userHospitalService.getHospitalOptionsByUserId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getMyHospitalOptions Tests ====================

    @Test
    @DisplayName("getMyHospitalOptions: Returns hospitals when role dataScopeType=hospitals")
    void getMyHospitalOptions_whenDataScopeTypeHospitals_shouldReturnHospitals() {
        List<Long> hospitalIds = List.of(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleService.getById(1L)).thenReturn(enabledRole);
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(hospitalIds);
        when(orgMapper.selectBatchIds(hospitalIds)).thenReturn(List.of(enabledHospital));

        List<OrgVO> result = userHospitalService.getMyHospitalOptions(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Hospital 1", result.get(0).getOrgName());
    }

    @Test
    @DisplayName("getMyHospitalOptions: Returns empty list when role dataScopeType=org")
    void getMyHospitalOptions_whenDataScopeTypeOrg_shouldReturnEmptyList() {
        testUser.setRoleId(2L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleService.getById(2L)).thenReturn(disabledRole);

        List<OrgVO> result = userHospitalService.getMyHospitalOptions(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getMyHospitalOptions: Returns empty list when user has no role")
    void getMyHospitalOptions_whenNoRole_shouldReturnEmptyList() {
        testUser.setRoleId(null);
        when(userMapper.selectById(1L)).thenReturn(testUser);

        List<OrgVO> result = userHospitalService.getMyHospitalOptions(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getMyHospitalOptions: Throws exception when user not found")
    void getMyHospitalOptions_whenUserNotFound_shouldThrowException() {
        when(userMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userHospitalService.getMyHospitalOptions(999L)
        );
        assertEquals(ErrorCodeEnum.USER_NOT_FOUND.getCode(), exception.getCode());
    }
}