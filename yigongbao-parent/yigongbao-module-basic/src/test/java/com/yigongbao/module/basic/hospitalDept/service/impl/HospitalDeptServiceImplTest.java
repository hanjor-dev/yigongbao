package com.yigongbao.module.basic.hospitalDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.hospitalDept.dto.CreateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.dto.UpdateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.entity.HospitalDeptEntity;
import com.yigongbao.module.basic.hospitalDept.mapper.HospitalDeptMapper;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
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
 * 医院科室 Service 单元测试
 *
 * @author hanjor
 * @date 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("HospitalDeptService 单元测试")
class HospitalDeptServiceImplTest {

    @Mock
    private HospitalDeptMapper hospitalDeptMapper;

    @Mock
    private com.yigongbao.module.basic.code.service.CodeGeneratorService codeGeneratorService;

    @InjectMocks
    private HospitalDeptServiceImpl hospitalDeptService;

    private HospitalDeptEntity testEntity;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 mapper 注入
        Field baseMapperField = hospitalDeptService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(hospitalDeptService, hospitalDeptMapper);

        // 初始化测试数据
        testEntity = new HospitalDeptEntity();
        testEntity.setId(1L);
        testEntity.setHospitalDeptCode("HDEPT-0001");
        testEntity.setHospitalDeptName("骨科");
        testEntity.setSort(1);
        testEntity.setStatus(1);
        testEntity.setCreateTime(LocalDateTime.now());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 科室存在时返回VO")
    void getById_whenExists_shouldReturnVO() {
        when(hospitalDeptMapper.selectById(1L)).thenReturn(testEntity);

        HospitalDeptVO vo = hospitalDeptService.getById(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("骨科", vo.getHospitalDeptName());
        assertEquals("正常", vo.getStatusName());
    }

    @Test
    @DisplayName("getById: 科室不存在时抛出异常")
    void getById_whenNotExists_shouldThrowException() {
        when(hospitalDeptMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> hospitalDeptService.getById(999L)
        );
        assertEquals(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建成功")
    void create_shouldSuccess() {
        CreateHospitalDeptDTO dto = new CreateHospitalDeptDTO();
        dto.setHospitalDeptName("口腔科");

        when(hospitalDeptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(codeGeneratorService.generate("HDEPT_NO")).thenReturn("HDEPT-0002");

        hospitalDeptService.create(dto);

        verify(hospitalDeptMapper, times(1)).insert(any(HospitalDeptEntity.class));
    }

    @Test
    @DisplayName("create: 科室名称已存在时抛出异常")
    void create_whenNameExists_shouldThrowException() {
        CreateHospitalDeptDTO dto = new CreateHospitalDeptDTO();
        dto.setHospitalDeptName("骨科");

        when(hospitalDeptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> hospitalDeptService.create(dto)
        );
        assertEquals(ErrorCodeEnum.HOSPITAL_DEPT_EXISTS.getCode(), exception.getCode());
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新成功")
    void update_shouldSuccess() {
        UpdateHospitalDeptDTO dto = new UpdateHospitalDeptDTO();
        dto.setHospitalDeptName("神经外科");

        when(hospitalDeptMapper.selectById(1L)).thenReturn(testEntity);
        when(hospitalDeptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        hospitalDeptService.update(1L, dto);

        verify(hospitalDeptMapper, times(1)).updateById(any(HospitalDeptEntity.class));
    }

    @Test
    @DisplayName("update: 科室不存在时抛出异常")
    void update_whenNotExists_shouldThrowException() {
        UpdateHospitalDeptDTO dto = new UpdateHospitalDeptDTO();
        dto.setHospitalDeptName("神经外科");

        when(hospitalDeptMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> hospitalDeptService.update(999L, dto)
        );
        assertEquals(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== remove 测试 ====================

    @Test
    @DisplayName("remove: 删除成功")
    void remove_shouldSuccess() {
        when(hospitalDeptMapper.selectById(1L)).thenReturn(testEntity);

        hospitalDeptService.remove(1L);

        verify(hospitalDeptMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("remove: 科室不存在时抛出异常")
    void remove_whenNotExists_shouldThrowException() {
        when(hospitalDeptMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> hospitalDeptService.remove(999L)
        );
        assertEquals(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() {
        when(hospitalDeptMapper.selectById(1L)).thenReturn(testEntity);

        hospitalDeptService.updateStatus(1L, 0);

        verify(hospitalDeptMapper, times(1)).updateById(any(HospitalDeptEntity.class));
    }

    // ==================== listAll 测试 ====================

    @Test
    @DisplayName("listAll: 返回所有科室")
    void listAll_shouldReturnAll() {
        when(hospitalDeptMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testEntity));

        List<HospitalDeptVO> list = hospitalDeptService.listAll(null, null);

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("listAll: 无数据时返回空列表")
    void listAll_whenEmpty_shouldReturnEmptyList() {
        when(hospitalDeptMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<HospitalDeptVO> list = hospitalDeptService.listAll(null, null);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== listDepts 测试 ====================

    @Test
    @DisplayName("listDepts: 分页查询返回数据")
    void listDepts_shouldReturnPageData() {
        Page<HospitalDeptEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(testEntity));
        page.setTotal(1);
        when(hospitalDeptMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<HospitalDeptVO> result = hospitalDeptService.listDepts(1, 10, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("listDepts: 无数据时返回空分页")
    void listDepts_whenEmpty_shouldReturnEmptyPage() {
        Page<HospitalDeptEntity> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(hospitalDeptMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        IPage<HospitalDeptVO> result = hospitalDeptService.listDepts(1, 10, null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== updateStatus 失败场景 ====================

    @Test
    @DisplayName("updateStatus: 数据不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        when(hospitalDeptMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> hospitalDeptService.updateStatus(999L, 0)
        );
        assertEquals(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND.getCode(), exception.getCode());
    }
}
