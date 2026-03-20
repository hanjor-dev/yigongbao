package com.yigongbao.module.system.user.service.impl;

import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import com.yigongbao.module.system.user.entity.UserHospitalEntity;
import com.yigongbao.module.system.user.mapper.UserHospitalMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserHospitalServiceImpl 单元测试")
class UserHospitalServiceImplTest {

    @Mock
    private UserHospitalMapper userHospitalMapper;

    @Mock
    private HospitalMapper hospitalMapper;

    @InjectMocks
    private UserHospitalServiceImpl userHospitalService;

    private HospitalEntity hospitalEntity;

    @BeforeEach
    void setUp() {
        hospitalEntity = new HospitalEntity();
        hospitalEntity.setId(1L);
        hospitalEntity.setHospitalName("测试医院");
        hospitalEntity.setHospitalCode("HOS-001");
        hospitalEntity.setStatus(1);
    }

    // ==================== getHospitalsByUserId 测试 ====================

    @Test
    @DisplayName("getHospitalsByUserId: 有关联医院时返回列表")
    void getHospitalsByUserId_whenHasHospitals_shouldReturnList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(List.of(1L));
        when(hospitalMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(hospitalEntity));
        List<HospitalVO> result = userHospitalService.getHospitalsByUserId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试医院", result.get(0).getHospitalName());
        verify(userHospitalMapper, times(1)).selectHospitalIdsByUserId(1L);
    }

    @Test
    @DisplayName("getHospitalsByUserId: 无关联医院时返回空列表")
    void getHospitalsByUserId_whenNoHospitals_shouldReturnEmptyList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(1L)).thenReturn(List.of());
        List<HospitalVO> result = userHospitalService.getHospitalsByUserId(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getHospitalsByUserId: 用户ID不存在时返回空列表")
    void getHospitalsByUserId_whenUserNotExists_shouldReturnEmptyList() {
        when(userHospitalMapper.selectHospitalIdsByUserId(999L)).thenReturn(null);
        List<HospitalVO> result = userHospitalService.getHospitalsByUserId(999L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== assignHospitals 测试 ====================

    @Test
    @DisplayName("assignHospitals: 正常分配医院")
    void assignHospitals_shouldAssignHospitals() {
        doNothing().when(userHospitalMapper).deleteByUserId(1L);
        userHospitalService.assignHospitals(1L, List.of(1L, 2L));
        verify(userHospitalMapper, times(1)).deleteByUserId(1L);
        verify(userHospitalMapper, times(2)).insert(any(UserHospitalEntity.class));
    }

    @Test
    @DisplayName("assignHospitals: hospitalIds为空时只删除旧关联")
    void assignHospitals_whenEmpty_shouldOnlyDelete() {
        doNothing().when(userHospitalMapper).deleteByUserId(1L);
        userHospitalService.assignHospitals(1L, List.of());
        verify(userHospitalMapper, times(1)).deleteByUserId(1L);
        verify(userHospitalMapper, never()).insert(any(UserHospitalEntity.class));
    }

    @Test
    @DisplayName("assignHospitals: hospitalIds为null时只删除旧关联")
    void assignHospitals_whenNull_shouldOnlyDelete() {
        doNothing().when(userHospitalMapper).deleteByUserId(1L);
        userHospitalService.assignHospitals(1L, null);
        verify(userHospitalMapper, times(1)).deleteByUserId(1L);
        verify(userHospitalMapper, never()).insert(any(UserHospitalEntity.class));
    }
}
