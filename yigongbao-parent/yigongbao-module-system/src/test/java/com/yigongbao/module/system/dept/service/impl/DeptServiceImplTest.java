package com.yigongbao.module.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.mapper.DeptMapper;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dept.vo.DeptVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DeptService 单元测试
 *
 * @author hanjor
 * @date 2026-03-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("DeptService 单元测试")
class DeptServiceImplTest {

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private OrgService orgService;

    @InjectMocks
    private DeptServiceImpl deptService;

    private DeptEntity testEntity;
    private CreateDeptDTO createDTO;
    private UpdateDeptDTO updateDTO;
    private OrgEntity testOrg;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 deptMapper 注入到 ServiceImpl 的 baseMapper 字段中
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(deptService, deptMapper);

        LocalDateTime now = LocalDateTime.now();

        // 初始化测试机构实体
        testOrg = new OrgEntity();
        testOrg.setId(1L);
        testOrg.setOrgName("测试机构");
        testOrg.setOrgCode("ORG-P-001");
        testOrg.setStatus(1);

        // 初始化测试部门实体
        testEntity = new DeptEntity();
        testEntity.setId(1L);
        testEntity.setDeptName("研发部");
        testEntity.setDeptCode("DEPT-001");
        testEntity.setOrgId(1L);
        testEntity.setLeaderUserId(null);
        testEntity.setStatus(1);
        testEntity.setCreateTime(now);
        testEntity.setUpdateTime(now);

        // 初始化创建DTO
        createDTO = new CreateDeptDTO();
        createDTO.setDeptName("市场部");
        createDTO.setOrgId(1L);
        createDTO.setLeaderUserId(null);
        createDTO.setRemark("市场部门");

        // 初始化更新DTO
        updateDTO = new UpdateDeptDTO();
        updateDTO.setDeptName("研发部");
        updateDTO.setLeaderUserId(100L);
        updateDTO.setRemark("研发部门");
    }

    // ==================== listDept 测试 ====================

    @Test
    @DisplayName("listDept: 分页查询成功")
    void listDept_shouldReturnPageData() {
        // 准备
        Page<DeptEntity> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(testEntity));

        when(deptMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(orgService.getById(1L)).thenReturn(testOrg);

        // 执行
        IPage<DeptVO> result = deptService.listDept(1, 10, 1L, null, null);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(deptMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listDept: 无数据时返回空列表")
    void listDept_whenNoData_shouldReturnEmptyList() {
        // 准备
        Page<DeptEntity> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(Collections.emptyList());

        when(deptMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // 执行
        IPage<DeptVO> result = deptService.listDept(1, 10, null, null, null);

        // 断言
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getDeptById 测试 ====================

    @Test
    @DisplayName("getDeptById: 存在数据时返回VO")
    void getDeptById_whenExists_shouldReturnData() {
        // 准备
        when(deptMapper.selectById(1L)).thenReturn(testEntity);
        when(orgService.getById(1L)).thenReturn(testOrg);

        // 执行
        DeptVO result = deptService.getDeptById(1L);

        // 断言
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("研发部", result.getDeptName());
        assertEquals("测试机构", result.getOrgName());
    }

    @Test
    @DisplayName("getDeptById: 数据不存在时抛出异常")
    void getDeptById_whenNotExists_shouldThrowException() {
        // 准备
        when(deptMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.getDeptById(999L)
        );
        assertEquals(ErrorCodeEnum.DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createDept 测试 ====================

    @Test
    @DisplayName("createDept: 创建成功")
    void createDept_shouldSuccess() {
        // 准备
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(deptMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(deptMapper.insert(any(DeptEntity.class))).thenReturn(1);

        // 执行
        deptService.createDept(createDTO);

        // 断言
        verify(deptMapper, times(1)).insert(any(DeptEntity.class));
    }

    @Test
    @DisplayName("createDept: 所属机构不存在时抛出异常")
    void createDept_whenOrgNotExists_shouldThrowException() {
        // 准备
        when(orgService.getById(999L)).thenReturn(null);

        // 设置一个不存在的机构ID
        CreateDeptDTO dto = new CreateDeptDTO();
        dto.setDeptName("测试部门");
        dto.setOrgId(999L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.createDept(dto)
        );
        assertEquals(ErrorCodeEnum.ORG_NOT_FOUND_FOR_DEPT.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createDept: 部门名称已存在时抛出异常")
    void createDept_whenNameExists_shouldThrowException() {
        // 准备
        when(orgService.getById(1L)).thenReturn(testOrg);
        when(deptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.createDept(createDTO)
        );
        assertEquals(ErrorCodeEnum.DEPT_EXISTS.getCode(), exception.getCode());
    }

    // ==================== updateDept 测试 ====================

    @Test
    @DisplayName("updateDept: 更新成功")
    void updateDept_shouldSuccess() {
        // 准备
        when(deptMapper.selectById(1L)).thenReturn(testEntity);
        when(deptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(deptMapper.updateById(any(DeptEntity.class))).thenReturn(1);

        // 执行
        deptService.updateDept(1L, updateDTO);

        // 断言
        verify(deptMapper, times(1)).updateById(any(DeptEntity.class));
    }

    @Test
    @DisplayName("updateDept: 部门不存在时抛出异常")
    void updateDept_whenNotExists_shouldThrowException() {
        // 准备
        when(deptMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.updateDept(999L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateDept: 更新时部门名称与其他部门重复时抛出异常")
    void updateDept_whenNameDuplicated_shouldThrowException() {
        // 准备：更新DTO的部门名称与原部门不同
        UpdateDeptDTO duplicateDTO = new UpdateDeptDTO();
        duplicateDTO.setDeptName("其他部门名称"); // 与testEntity的"研发部"不同

        // 模拟：查询到原部门 + 名称重复（selectCount > 0）
        when(deptMapper.selectById(1L)).thenReturn(testEntity);
        when(deptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.updateDept(1L, duplicateDTO)
        );
        assertEquals(ErrorCodeEnum.DEPT_EXISTS.getCode(), exception.getCode());
    }

    // ==================== removeDept 测试 ====================

    @Test
    @DisplayName("removeDept: 删除成功")
    void removeDept_shouldSuccess() {
        // 准备
        when(deptMapper.selectById(1L)).thenReturn(testEntity);
        when(deptMapper.deleteById(1L)).thenReturn(1);

        // 执行
        deptService.removeDept(1L);

        // 断言
        verify(deptMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("removeDept: 部门不存在时抛出异常")
    void removeDept_whenNotExists_shouldThrowException() {
        // 准备
        when(deptMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.removeDept(999L)
        );
        assertEquals(ErrorCodeEnum.DEPT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() {
        // 准备
        when(deptMapper.selectById(1L)).thenReturn(testEntity);
        when(deptMapper.updateById(any(DeptEntity.class))).thenReturn(1);

        // 执行
        deptService.updateStatus(1L, 0);

        // 断言
        verify(deptMapper, times(1)).updateById(any(DeptEntity.class));
    }

    @Test
    @DisplayName("updateStatus: 部门不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        // 准备
        when(deptMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deptService.updateStatus(999L, 0)
        );
        assertEquals(ErrorCodeEnum.DEPT_NOT_FOUND.getCode(), exception.getCode());
    }
}
