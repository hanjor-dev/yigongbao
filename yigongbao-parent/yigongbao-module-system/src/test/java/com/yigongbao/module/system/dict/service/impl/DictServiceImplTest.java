package com.yigongbao.module.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.dict.convert.DictConvert;
import com.yigongbao.module.system.dict.dto.CreateDictDTO;
import com.yigongbao.module.system.dict.dto.UpdateDictDTO;
import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.mapper.DictMapper;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * DictService 单元测试
 *
 * @author hanjor
 * @date 2026-03-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("DictService 单元测试")
class DictServiceImplTest {

    @Mock
    private DictMapper dictMapper;

    @InjectMocks
    private DictServiceImpl dictService;

    private DictEntity rootEntity;
    private DictEntity childEntity;
    private CreateDictDTO createDTO;
    private UpdateDictDTO updateDTO;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(dictService, dictMapper);

        // 初始化根节点实体
        rootEntity = new DictEntity();
        rootEntity.setId(1L);
        rootEntity.setParentId(0L);
        rootEntity.setDictCode("1");
        rootEntity.setDictName("机构类型");
        rootEntity.setLevel(1);
        rootEntity.setSort(0);
        rootEntity.setStatus(1);
        rootEntity.setCreateTime(LocalDateTime.now());

        // 初始化子节点实体
        childEntity = new DictEntity();
        childEntity.setId(2L);
        childEntity.setParentId(1L);
        childEntity.setDictCode("1.1");
        childEntity.setDictName("生产企业");
        childEntity.setDictValue("production");
        childEntity.setLevel(2);
        childEntity.setSort(0);
        childEntity.setStatus(1);
        childEntity.setCreateTime(LocalDateTime.now());

        // 初始化创建DTO
        createDTO = new CreateDictDTO();
        createDTO.setParentId(0L);
        createDTO.setDictName("新字典类型");
        createDTO.setStatus(1);

        // 初始化更新DTO
        updateDTO = new UpdateDictDTO();
        updateDTO.setDictName("更新后的字典名称");
        updateDTO.setStatus(1);
    }

    // ==================== listType 测试 ====================

    @Test
    @DisplayName("listType: 查询字典类型列表成功")
    void listType_shouldReturnRootNodes() {
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(rootEntity));

        List<DictVO> result = dictService.listType();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("机构类型", result.get(0).getDictName());
        verify(dictMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("listType: 无数据时返回空列表")
    void listType_whenNoData_shouldReturnEmptyList() {
        when(dictMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<DictVO> result = dictService.listType();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== listByTypeCode 测试 ====================

    @Test
    @DisplayName("listByTypeCode: 根据类型编码查询成功")
    void listByTypeCode_shouldReturnDataList() {
        // 第一次调用：查询所有数据；第二次调用：筛选子节点
        when(dictMapper.selectList(any()))
                .thenReturn(Arrays.asList(rootEntity, childEntity));

        List<DictVO> result = dictService.listByTypeCode("1");

        assertNotNull(result);
        // 应该过滤出父ID为1的子节点
        assertTrue(result.stream().allMatch(vo -> vo.getParentId().equals(1L)));
    }

    @Test
    @DisplayName("listByTypeCode: 类型不存在时抛出异常")
    void listByTypeCode_whenTypeNotExists_shouldThrowException() {
        // 只返回一个子节点，没有根节点
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(childEntity));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.listByTypeCode("999")
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 存在数据时返回VO")
    void getById_whenExists_shouldReturnData() {
        when(dictMapper.selectById(1L)).thenReturn(rootEntity);

        DictVO result = dictService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("机构类型", result.getDictName());
    }

    @Test
    @DisplayName("getById: 数据不存在时抛出异常")
    void getById_whenNotExists_shouldThrowException() {
        when(dictMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.getById(999L)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建根节点成功")
    void create_rootNode_shouldSuccess() {
        when(dictMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dictMapper.selectCount(any())).thenReturn(0L);
        when(dictMapper.insert(any(DictEntity.class))).thenReturn(1);

        dictService.create(createDTO);

        verify(dictMapper, times(1)).insert(any(DictEntity.class));
    }

    @Test
    @DisplayName("create: 创建子节点成功")
    void create_childNode_shouldSuccess() {
        createDTO.setParentId(1L);
        when(dictMapper.selectById(1L)).thenReturn(rootEntity);
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(childEntity));
        when(dictMapper.selectCount(any())).thenReturn(0L);
        when(dictMapper.insert(any(DictEntity.class))).thenReturn(1);

        dictService.create(createDTO);

        verify(dictMapper, times(1)).insert(any(DictEntity.class));
    }

    @Test
    @DisplayName("create: 字典编码已存在时抛出异常")
    void create_whenDictCodeExists_shouldThrowException() {
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(rootEntity));
        when(dictMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.create(createDTO)
        );
        assertEquals(ErrorCodeEnum.DICT_CODE_EXISTS.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("create: 字典名称在同一父节点下已存在时抛出异常")
    void create_whenDictNameExists_shouldThrowException() {
        when(dictMapper.selectList(any())).thenReturn(Collections.emptyList());
        // 第一次调用count检查编码（返回0），第二次调用count检查名称（返回1）
        when(dictMapper.selectCount(any()))
                .thenReturn(0L)
                .thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.create(createDTO)
        );
        assertEquals(ErrorCodeEnum.DICT_NAME_EXISTS.getCode(), exception.getCode());
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新成功")
    void update_shouldSuccess() {
        when(dictMapper.selectById(1L)).thenReturn(rootEntity);
        when(dictMapper.selectCount(any())).thenReturn(0L);
        when(dictMapper.updateById(any(DictEntity.class))).thenReturn(1);

        dictService.update(1L, updateDTO);

        verify(dictMapper, times(1)).updateById(any(DictEntity.class));
    }

    @Test
    @DisplayName("update: 数据不存在时抛出异常")
    void update_whenNotExists_shouldThrowException() {
        when(dictMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.update(999L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("update: 字典名称在同一父节点下已存在时抛出异常")
    void update_whenDictNameExists_shouldThrowException() {
        when(dictMapper.selectById(1L)).thenReturn(rootEntity);
        when(dictMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.update(1L, updateDTO)
        );
        assertEquals(ErrorCodeEnum.DICT_NAME_EXISTS.getCode(), exception.getCode());
    }

    // ==================== remove 测试 ====================

    @Test
    @DisplayName("remove: 删除成功（无子节点）")
    void remove_shouldSuccess() {
        when(dictMapper.selectById(2L)).thenReturn(childEntity);
        when(dictMapper.selectCount(any())).thenReturn(0L);
        when(dictMapper.deleteById(2L)).thenReturn(1);

        dictService.remove(2L);

        verify(dictMapper, times(1)).deleteById(2L);
    }

    @Test
    @DisplayName("remove: 有子节点时抛出异常")
    void remove_whenHasChildren_shouldThrowException() {
        when(dictMapper.selectById(1L)).thenReturn(rootEntity);
        when(dictMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.remove(1L)
        );
        assertEquals(ErrorCodeEnum.DATA_HAS_CHILDREN.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("remove: 数据不存在时抛出异常")
    void remove_whenNotExists_shouldThrowException() {
        when(dictMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.remove(999L)
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 修改状态成功")
    void updateStatus_shouldSuccess() {
        when(dictMapper.selectById(1L)).thenReturn(rootEntity);
        when(dictMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dictMapper.update(any(), any())).thenReturn(1);

        dictService.updateStatus(1L, 0);

        verify(dictMapper, times(1)).update(any(), any());
    }

    // ==================== listTree 测试 ====================

    @Test
    @DisplayName("listTree: 返回树形结构")
    void listTree_shouldReturnTreeStructure() {
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(rootEntity, childEntity));

        List<DictVO> result = dictService.listTree();

        assertNotNull(result);
        // 根节点应该在结果中
        assertTrue(result.stream().anyMatch(vo -> vo.getDictCode().equals("1")));
    }

    @Test
    @DisplayName("listTreeByTypeCode: 返回指定类型的树形结构")
    void listTreeByTypeCode_shouldReturnTreeStructure() {
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(rootEntity, childEntity));

        List<DictVO> result = dictService.listTreeByTypeCode("1");

        assertNotNull(result);
        // 根节点应该在结果中
        assertTrue(result.stream().anyMatch(vo -> vo.getDictCode().equals("1")));
    }

    @Test
    @DisplayName("listTreeByTypeCode: 类型不存在时抛出异常")
    void listTreeByTypeCode_whenTypeNotExists_shouldThrowException() {
        when(dictMapper.selectList(any())).thenReturn(Arrays.asList(childEntity));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> dictService.listTreeByTypeCode("999")
        );
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== listOptions 测试 ====================

    @Test
    @DisplayName("listOptions: 返回叶子节点")
    void listOptions_shouldReturnLeafNodes() {
        when(dictMapper.selectList(any()))
                .thenReturn(Arrays.asList(rootEntity, childEntity));

        List<DictVO> result = dictService.listOptions("1");

        assertNotNull(result);
        // 应该有叶子节点
        assertTrue(result.stream().anyMatch(vo -> vo.getDictCode().equals("1.1")));
    }
}
