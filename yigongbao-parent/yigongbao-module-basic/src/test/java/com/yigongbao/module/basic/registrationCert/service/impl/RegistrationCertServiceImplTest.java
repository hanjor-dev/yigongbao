package com.yigongbao.module.basic.registrationCert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.registrationCert.dto.CreateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.dto.RegistrationCertPageDTO;
import com.yigongbao.module.basic.registrationCert.dto.UpdateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.entity.RegistrationCertEntity;
import com.yigongbao.module.basic.registrationCert.mapper.RegistrationCertMapper;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 注册证 Service 单元测试
 *
 * @author hanjor
 * @date 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("RegistrationCertService 单元测试")
class RegistrationCertServiceImplTest {

    @Mock
    private RegistrationCertMapper registrationCertMapper;

    @InjectMocks
    private RegistrationCertServiceImpl registrationCertService;

    private RegistrationCertEntity testEntity;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = registrationCertService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(registrationCertService, registrationCertMapper);

        testEntity = new RegistrationCertEntity();
        testEntity.setId(1L);
        testEntity.setCertCode("REG-20260001");
        testEntity.setCertName("医疗器械注册证");
        testEntity.setValidFrom(LocalDate.of(2026, 1, 1));
        testEntity.setValidTo(LocalDate.of(2028, 12, 31));
        testEntity.setStatus(1);
        testEntity.setCreateTime(LocalDateTime.now());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 注册证存在时返回VO")
    void getById_whenExists_shouldReturnVO() {
        when(registrationCertMapper.selectById(1L)).thenReturn(testEntity);

        RegistrationCertVO vo = registrationCertService.getById(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("REG-20260001", vo.getCertCode());
    }

    @Test
    @DisplayName("getById: 注册证不存在时抛出异常")
    void getById_whenNotExists_shouldThrowException() {
        when(registrationCertMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registrationCertService.getById(999L)
        );
        assertEquals(ErrorCodeEnum.CERT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建成功")
    void create_shouldSuccess() {
        CreateRegistrationCertDTO dto = new CreateRegistrationCertDTO();
        dto.setCertCode("REG-NEW001");
        dto.setCertName("新注册证");
        dto.setValidFrom(LocalDate.of(2026, 1, 1));
        dto.setValidTo(LocalDate.of(2030, 12, 31));

        when(registrationCertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        registrationCertService.create(dto);

        verify(registrationCertMapper, times(1)).insert(any(RegistrationCertEntity.class));
    }

    @Test
    @DisplayName("create: 编码已存在时抛出异常")
    void create_whenCodeExists_shouldThrowException() {
        CreateRegistrationCertDTO dto = new CreateRegistrationCertDTO();
        dto.setCertCode("REG-20260001");
        dto.setCertName("新注册证");

        when(registrationCertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registrationCertService.create(dto)
        );
        assertEquals(ErrorCodeEnum.CERT_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("create: 有效期已过期时自动设为禁用状态")
    void create_whenExpired_shouldSetDisabledStatus() {
        CreateRegistrationCertDTO dto = new CreateRegistrationCertDTO();
        dto.setCertCode("REG-EXPIRED");
        dto.setCertName("过期注册证");
        dto.setValidFrom(LocalDate.of(2020, 1, 1));
        dto.setValidTo(LocalDate.of(2023, 12, 31));

        when(registrationCertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        registrationCertService.create(dto);

        ArgumentCaptor<RegistrationCertEntity> captor = ArgumentCaptor.forClass(RegistrationCertEntity.class);
        verify(registrationCertMapper, times(1)).insert(captor.capture());
        assertEquals(StatusConstants.DISABLED, captor.getValue().getStatus());
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新成功")
    void update_shouldSuccess() {
        UpdateRegistrationCertDTO dto = new UpdateRegistrationCertDTO();
        dto.setCertName("更新后的注册证名称");

        when(registrationCertMapper.selectById(1L)).thenReturn(testEntity);
        when(registrationCertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        registrationCertService.update(1L, dto);

        verify(registrationCertMapper, times(1)).updateById(any(RegistrationCertEntity.class));
    }

    @Test
    @DisplayName("update: 注册证不存在时抛出异常")
    void update_whenNotExists_shouldThrowException() {
        UpdateRegistrationCertDTO dto = new UpdateRegistrationCertDTO();
        dto.setCertName("更新后的名称");

        when(registrationCertMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registrationCertService.update(999L, dto)
        );
        assertEquals(ErrorCodeEnum.CERT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("update: 编码已存在时抛出异常")
    void update_whenCodeExists_shouldThrowException() {
        UpdateRegistrationCertDTO dto = new UpdateRegistrationCertDTO();
        dto.setCertCode("REG-DUPLICATE");

        when(registrationCertMapper.selectById(1L)).thenReturn(testEntity);
        when(registrationCertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registrationCertService.update(1L, dto)
        );
        assertEquals(ErrorCodeEnum.CERT_EXISTS.getCode(), exception.getCode());
    }

    // ==================== remove 测试 ====================

    @Test
    @DisplayName("remove: 删除成功")
    void remove_shouldSuccess() {
        when(registrationCertMapper.selectById(1L)).thenReturn(testEntity);

        registrationCertService.remove(1L);

        verify(registrationCertMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("remove: 注册证不存在时抛出异常")
    void remove_whenNotExists_shouldThrowException() {
        when(registrationCertMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registrationCertService.remove(999L)
        );
        assertEquals(ErrorCodeEnum.CERT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== listCerts 分页测试 ====================

    @Test
    @DisplayName("listCerts: 分页查询返回数据")
    void listCerts_shouldReturnPageData() {
        Page<RegistrationCertEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testEntity));
        page.setTotal(1);
        when(registrationCertMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<RegistrationCertVO> result = registrationCertService.listCerts(new RegistrationCertPageDTO());

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("listCerts: 无数据时返回空分页")
    void listCerts_whenEmpty_shouldReturnEmptyPage() {
        Page<RegistrationCertEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(registrationCertMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<RegistrationCertVO> result = registrationCertService.listCerts(new RegistrationCertPageDTO());

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== listValidCerts 测试 ====================

    @Test
    @DisplayName("listValidCerts: 返回有效注册证列表")
    void listValidCerts_shouldReturnValidList() {
        when(registrationCertMapper.selectList(any()))
                .thenReturn(Collections.singletonList(testEntity));

        List<RegistrationCertVO> list = registrationCertService.listValidCerts();

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("listValidCerts: 无数据时返回空列表")
    void listValidCerts_whenEmpty_shouldReturnEmptyList() {
        when(registrationCertMapper.selectList(any()))
                .thenReturn(Collections.emptyList());

        List<RegistrationCertVO> list = registrationCertService.listValidCerts();

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== refreshExpiredStatus 测试 ====================

    @Test
    @DisplayName("refreshExpiredStatus: 过期证自动更新状态")
    void refreshExpiredStatus_shouldUpdateExpiredCerts() {
        RegistrationCertEntity expiredEntity = new RegistrationCertEntity();
        expiredEntity.setId(2L);
        expiredEntity.setCertCode("REG-EXPIRED");
        expiredEntity.setStatus(StatusConstants.NORMAL);
        expiredEntity.setValidTo(LocalDate.now().minusDays(1));

        when(registrationCertMapper.selectList(any()))
                .thenReturn(Collections.singletonList(expiredEntity));

        registrationCertService.refreshExpiredStatus();

        ArgumentCaptor<RegistrationCertEntity> captor = ArgumentCaptor.forClass(RegistrationCertEntity.class);
        verify(registrationCertMapper, times(1)).updateById(captor.capture());
        assertEquals(StatusConstants.DISABLED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("refreshExpiredStatus: 未过期证状态不变")
    void refreshExpiredStatus_shouldNotChangeValidCerts() {
        when(registrationCertMapper.selectList(any()))
                .thenReturn(Collections.singletonList(testEntity));

        registrationCertService.refreshExpiredStatus();

        verify(registrationCertMapper, never()).updateById(any(RegistrationCertEntity.class));
    }

    @Test
    @DisplayName("refreshExpiredStatus: 无数据时正常执行")
    void refreshExpiredStatus_whenEmpty_shouldRunNormally() {
        when(registrationCertMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> registrationCertService.refreshExpiredStatus());
    }
}
