package com.yigongbao.module.system.org.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.org.vo.OrgVO;
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
 * OrgService 单元测试
 *
 * @author hanjor
 * @date 2026-03-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("OrgService 单元测试")
class OrgServiceImplTest {

    @Mock
    private OrgMapper orgMapper;

    @Mock
    private DictService dictService;

    @InjectMocks
    private OrgServiceImpl orgService;

    private OrgEntity testEntity;
    private CreateOrgDTO createDTO;
    private UpdateOrgDTO updateDTO;
    private DictVO orgTypeVO;

    @BeforeEach
    void setUp() throws Exception {
        // 通过反射将 mock 的 orgMapper 注入到 ServiceImpl 的 baseMapper 字段中
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(orgService, orgMapper);

        LocalDateTime now = LocalDateTime.now();

        // 初始化测试机构实体
        testEntity = new OrgEntity();
        testEntity.setId(1L);
        testEntity.setOrgName("测试机构");
        testEntity.setOrgCode("ORG-P-001");
        testEntity.setOrgType(1);
        testEntity.setContact("张三");
        testEntity.setPhone("13800138000");
        testEntity.setStatus(1);
        testEntity.setCreateTime(now);
        testEntity.setUpdateTime(now);

        // 初始化创建DTO
        createDTO = new CreateOrgDTO();
        createDTO.setOrgName("新机构");
        createDTO.setOrgType(1);
        createDTO.setContact("李四");
        createDTO.setPhone("13900139000");
        createDTO.setAreaName("北京市");
        createDTO.setAddress("朝阳区xxx");
        createDTO.setAgentArea("华东区");

        // 初始化更新DTO
        updateDTO = new UpdateOrgDTO();
        updateDTO.setOrgName("更新后的机构名称");
        updateDTO.setContact("王五");
        updateDTO.setPhone("13700137000");

        // 初始化机构类型字典
        orgTypeVO = new DictVO();
        orgTypeVO.setId(2L);
        orgTypeVO.setDictCode("1.1");
        orgTypeVO.setDictName("生产企业");
        orgTypeVO.setDictValue(DictCodeConstants.ORG_TYPE);
    }

    // ==================== listOrg 测试 ====================

    @Test
    @DisplayName("listOrg: 分页查询成功")
    void listOrg_shouldReturnPageData() {
        // 准备
        Page<OrgEntity> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(testEntity));

        when(orgMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(dictService.listByTypeCode(DictCodeConstants.ORG_TYPE)).thenReturn(List.of(orgTypeVO));

        // 执行
        IPage<OrgVO> result = orgService.listOrg(1, 10, null, null, null, null);

        // 断言
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(orgMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("listOrg: 无数据时返回空列表")
    void listOrg_whenNoData_shouldReturnEmptyList() {
        // 准备
        Page<OrgEntity> page = new Page<>(1, 10);
        page.setTotal(0);
        page.setRecords(Collections.emptyList());

        when(orgMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        // 执行
        IPage<OrgVO> result = orgService.listOrg(1, 10, null, null, null, null);

        // 断言
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getOrgById 测试 ====================

    @Test
    @DisplayName("getOrgById: 存在数据时返回VO")
    void getOrgById_whenExists_shouldReturnData() {
        // 准备
        when(orgMapper.selectById(1L)).thenReturn(testEntity);
        when(dictService.listByTypeCode(DictCodeConstants.ORG_TYPE)).thenReturn(List.of(orgTypeVO));

        // 执行
        OrgVO result = orgService.getOrgById(1L);

        // 断言
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试机构", result.getOrgName());
    }

    @Test
    @DisplayName("getOrgById: 数据不存在时抛出异常")
    void getOrgById_whenNotExists_shouldThrowException() {
        // 准备
        when(orgMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.getOrgById(999L)
        );
        assertEquals(ErrorCodeEnum.ORG_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== createOrg 测试 ====================

    @Test
    @DisplayName("createOrg: 创建成功")
    void createOrg_shouldSuccess() {
        // 准备
        when(orgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dictService.listByTypeCode(DictCodeConstants.ORG_TYPE)).thenReturn(List.of(orgTypeVO));
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(orgMapper.insert(any(OrgEntity.class))).thenReturn(1);

        // 执行
        orgService.createOrg(createDTO);

        // 断言
        verify(orgMapper, times(1)).insert(any(OrgEntity.class));
    }

    @Test
    @DisplayName("createOrg: 机构名称已存在时抛出异常")
    void createOrg_whenNameExists_shouldThrowException() {
        // 准备
        when(orgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.createOrg(createDTO)
        );
        assertEquals(ErrorCodeEnum.ORG_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("createOrg: 机构类型不存在时抛出异常")
    void createOrg_whenTypeNotExists_shouldThrowException() {
        // 准备
        when(orgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dictService.listByTypeCode(DictCodeConstants.ORG_TYPE)).thenReturn(Collections.emptyList());

        // 创建DTO，设置一个不存在的类型
        CreateOrgDTO dto = new CreateOrgDTO();
        dto.setOrgName("测试");
        dto.setOrgType(999);
        dto.setContact("联系人");
        dto.setPhone("13800138000");

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.createOrg(dto)
        );
        assertEquals(ErrorCodeEnum.ORG_TYPE_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateOrg 测试 ====================

    @Test
    @DisplayName("updateOrg: 更新成功")
    void updateOrg_shouldSuccess() {
        // 准备
        when(orgMapper.selectById(1L)).thenReturn(testEntity);
        when(orgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(orgMapper.updateById(any(OrgEntity.class))).thenReturn(1);

        // 执行
        orgService.updateOrg(1L, updateDTO);

        // 断言
        verify(orgMapper, times(1)).updateById(any(OrgEntity.class));
    }

    @Test
    @DisplayName("updateOrg: 机构不存在时抛出异常")
    void updateOrg_whenNotExists_shouldThrowException() {
        // 准备
        when(orgMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.updateOrg(999L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.ORG_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("updateOrg: 更新时机构名称与其他机构重复时抛出异常")
    void updateOrg_whenNameDuplicated_shouldThrowException() {
        // 准备：更新DTO的机构名称与原机构不同
        UpdateOrgDTO duplicateDTO = new UpdateOrgDTO();
        duplicateDTO.setOrgName("其他机构名称"); // 与testEntity的"测试机构"不同

        // 模拟：查询到原机构 + 名称重复（selectCount > 0）
        when(orgMapper.selectById(1L)).thenReturn(testEntity);
        when(orgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.updateOrg(1L, duplicateDTO)
        );
        assertEquals(ErrorCodeEnum.ORG_EXISTS.getCode(), exception.getCode());
    }

    // ==================== removeOrg 测试 ====================

    @Test
    @DisplayName("removeOrg: 删除成功")
    void removeOrg_shouldSuccess() {
        // 准备
        when(orgMapper.selectById(1L)).thenReturn(testEntity);
        when(orgMapper.deleteById(1L)).thenReturn(1);

        // 执行
        orgService.removeOrg(1L);

        // 断言
        verify(orgMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("removeOrg: 机构不存在时抛出异常")
    void removeOrg_whenNotExists_shouldThrowException() {
        // 准备
        when(orgMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.removeOrg(999L)
        );
        assertEquals(ErrorCodeEnum.ORG_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() {
        // 准备
        when(orgMapper.selectById(1L)).thenReturn(testEntity);
        when(orgMapper.updateById(any(OrgEntity.class))).thenReturn(1);

        // 执行
        orgService.updateStatus(1L, 0);

        // 断言
        verify(orgMapper, times(1)).updateById(any(OrgEntity.class));
    }

    @Test
    @DisplayName("updateStatus: 机构不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        // 准备
        when(orgMapper.selectById(999L)).thenReturn(null);

        // 执行 & 断言
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orgService.updateStatus(999L, 0)
        );
        assertEquals(ErrorCodeEnum.ORG_NOT_FOUND.getCode(), exception.getCode());
    }
}
