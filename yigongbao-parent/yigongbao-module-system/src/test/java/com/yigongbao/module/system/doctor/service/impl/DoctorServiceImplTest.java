package com.yigongbao.module.system.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.system.doctor.dto.DoctorListDTO;
import com.yigongbao.module.system.doctor.dto.DoctorPageDTO;
import com.yigongbao.module.system.doctor.dto.DoctorSuggestDTO;
import com.yigongbao.module.system.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.system.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import com.yigongbao.module.system.doctor.mapper.DoctorMapper;
import com.yigongbao.module.system.doctor.vo.DoctorVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 医生 Service 单元测试
 *
 * @author hanjor
 * @date 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("DoctorService 单元测试")
class DoctorServiceImplTest {

    @Mock
    private DoctorMapper doctorMapper;

    @Mock
    private OrgService orgService;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private DoctorEntity testEntity;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = doctorService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(doctorService, doctorMapper);

        testEntity = new DoctorEntity();
        testEntity.setId(1L);
        testEntity.setDoctorName("张三");
        testEntity.setDoctorPhone("13800138000");
        testEntity.setHospitalId(1L);
        testEntity.setCreatorId(1L);
        testEntity.setOrderCount(0);
        testEntity.setStatus(1);
        testEntity.setCreateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("getById: 医生存在时返回VO")
    void getById_whenExists_shouldReturnVO() {
        when(doctorMapper.selectById(1L)).thenReturn(testEntity);
        DoctorVO vo = doctorService.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("张三", vo.getDoctorName());
    }

    @Test
    @DisplayName("getById: 医生不存在时抛出异常")
    void getById_whenNotExists_shouldThrowException() {
        when(doctorMapper.selectById(999L)).thenReturn(null);
        BusinessException exception = assertThrows(BusinessException.class, () -> doctorService.getById(999L));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("create: 创建成功")
    void create_shouldSuccess() {
        CreateDoctorDTO dto = new CreateDoctorDTO();
        dto.setDoctorName("李四");
        dto.setDoctorPhone("13900139000");
        dto.setHospitalId(1L);

        OrgEntity org = new OrgEntity();
        org.setOrgType("1.3");
        when(orgService.getById(1L)).thenReturn(org);

        doctorService.create(dto);
        verify(doctorMapper, times(1)).insert(any(DoctorEntity.class));
    }

    @Test
    @DisplayName("create: 医院不存在时抛出异常")
    void create_whenHospitalNotExists_shouldThrowException() {
        CreateDoctorDTO dto = new CreateDoctorDTO();
        dto.setDoctorName("李四");
        dto.setHospitalId(999L);
        when(orgService.getById(999L)).thenReturn(null);
        BusinessException exception = assertThrows(BusinessException.class, () -> doctorService.create(dto));
        assertEquals(ErrorCodeEnum.HOSPITAL_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("update: 更新成功")
    void update_shouldSuccess() {
        UpdateDoctorDTO dto = new UpdateDoctorDTO();
        dto.setDoctorName("王五");
        when(doctorMapper.selectById(1L)).thenReturn(testEntity);
        doctorService.update(1L, dto);
        verify(doctorMapper, times(1)).updateById(any(DoctorEntity.class));
    }

    @Test
    @DisplayName("update: 医生不存在时抛出异常")
    void update_whenNotExists_shouldThrowException() {
        UpdateDoctorDTO dto = new UpdateDoctorDTO();
        dto.setDoctorName("王五");
        when(doctorMapper.selectById(999L)).thenReturn(null);
        BusinessException exception = assertThrows(BusinessException.class, () -> doctorService.update(999L, dto));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("remove: 删除成功")
    void remove_shouldSuccess() {
        when(doctorMapper.selectById(1L)).thenReturn(testEntity);
        doctorService.remove(1L);
        verify(doctorMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("remove: 医生不存在时抛出异常")
    void remove_whenNotExists_shouldThrowException() {
        when(doctorMapper.selectById(999L)).thenReturn(null);
        BusinessException exception = assertThrows(BusinessException.class, () -> doctorService.remove(999L));
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("quickAdd: 医生不存在时创建成功")
    void quickAdd_whenNotExists_shouldCreate() {
        QuickAddDoctorDTO dto = new QuickAddDoctorDTO();
        dto.setDoctorName("新医生");
        dto.setHospitalId(1L);

        OrgEntity org = new OrgEntity();
        org.setOrgType("1.3");
        when(orgService.getById(1L)).thenReturn(org);
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        DoctorVO vo = doctorService.quickAdd(dto);
        assertNotNull(vo);
        verify(doctorMapper, times(1)).insert(any(DoctorEntity.class));
    }

    @Test
    @DisplayName("quickAdd: 医生已存在时返回现有医生")
    void quickAdd_whenExists_shouldReturnExisting() {
        QuickAddDoctorDTO dto = new QuickAddDoctorDTO();
        dto.setDoctorName("张三");
        dto.setHospitalId(1L);

        OrgEntity org = new OrgEntity();
        org.setOrgType("1.3");
        when(orgService.getById(1L)).thenReturn(org);
        when(doctorMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testEntity);

        DoctorVO vo = doctorService.quickAdd(dto);
        assertNotNull(vo);
        assertEquals("张三", vo.getDoctorName());
    }

    @Test
    @DisplayName("listAll: 返回所有医生")
    void listAll_shouldReturnAll() {
        when(doctorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(testEntity));
        List<DoctorVO> list = doctorService.listAll(new DoctorListDTO());
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("listDoctors: 分页查询返回数据")
    void listDoctors_shouldReturnPageData() {
        Page<DoctorEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testEntity));
        page.setTotal(1);
        when(doctorMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        IPage<DoctorVO> result = doctorService.listDoctors(new DoctorPageDTO());
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("updateStatus: 修改成功")
    void updateStatus_shouldSuccess() {
        when(doctorMapper.selectById(1L)).thenReturn(testEntity);
        doctorService.updateStatus(1L, 0);
        verify(doctorMapper, times(1)).updateById(any(DoctorEntity.class));
    }

    @Test
    @DisplayName("listByCreatorAndHospital: 返回历史医生列表")
    void listByCreatorAndHospital_shouldReturnList() {
        when(doctorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(testEntity));
        List<DoctorVO> result = doctorService.listByCreatorAndHospital(new DoctorSuggestDTO());
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
