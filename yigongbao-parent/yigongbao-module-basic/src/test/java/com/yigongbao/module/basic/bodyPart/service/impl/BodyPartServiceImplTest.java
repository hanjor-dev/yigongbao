package com.yigongbao.module.basic.bodyPart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.bodyPart.dto.CreateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.dto.UpdateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.entity.BodyPartEntity;
import com.yigongbao.module.basic.bodyPart.mapper.BodyPartMapper;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartOptionVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartVO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BodyPartServiceImpl 单元测试")
class BodyPartServiceImplTest {

    @Mock
    private BodyPartMapper bodyPartMapper;

    @Mock
    private CodeGeneratorService codeGeneratorService;

    @InjectMocks
    private BodyPartServiceImpl bodyPartService;

    private BodyPartEntity headEntity;
    private BodyPartEntity foreheadEntity;
    private CreateBodyPartDTO createDTO;
    private UpdateBodyPartDTO updateDTO;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(bodyPartService, bodyPartMapper);

        LocalDateTime now = LocalDateTime.now();

        headEntity = new BodyPartEntity();
        headEntity.setId(1L);
        headEntity.setParentId(0L);
        headEntity.setName("头部");
        headEntity.setCode("BP-0001");
        headEntity.setLevel(1);
        headEntity.setDesignerCode("A");
        headEntity.setSort(1);
        headEntity.setStatus(1);
        headEntity.setCreateTime(now);
        headEntity.setUpdateTime(now);

        foreheadEntity = new BodyPartEntity();
        foreheadEntity.setId(2L);
        foreheadEntity.setParentId(1L);
        foreheadEntity.setName("前额");
        foreheadEntity.setCode("BP-0002");
        foreheadEntity.setLevel(2);
        foreheadEntity.setDesignerCode("A");
        foreheadEntity.setSort(1);
        foreheadEntity.setStatus(1);
        foreheadEntity.setCreateTime(now);
        foreheadEntity.setUpdateTime(now);

        createDTO = new CreateBodyPartDTO();
        createDTO.setParentId(0L);
        createDTO.setName("躯干");
        createDTO.setDesignerCode("B");
        createDTO.setSort(2);

        updateDTO = new UpdateBodyPartDTO();
        updateDTO.setName("更新后的部位");
        updateDTO.setDesignerCode("C");
        updateDTO.setSort(3);
    }

    // ==================== getDetailById 测试 ====================

    @Nested
    @DisplayName("getDetailById 测试")
    class GetDetailByIdTests {

        @Test
        @DisplayName("getDetailById: 存在数据时返回详情")
        void getDetailById_whenExists_shouldReturnData() {
            when(bodyPartMapper.selectById(1L)).thenReturn(headEntity);
            BodyPartDetailVO result = bodyPartService.getDetailById(1L);
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("头部", result.getName());
            assertEquals("BP_001", result.getCode());
            assertEquals(1, result.getLevel());
        }

        @Test
        @DisplayName("getDetailById: 数据不存在时抛出异常")
        void getDetailById_whenNotExists_shouldThrowException() {
            when(bodyPartMapper.selectById(999L)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.getDetailById(999L));
            assertEquals(ErrorCodeEnum.BODY_PART_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("getDetailById: 有父级时正确返回父级名称")
        void getDetailById_whenHasParent_shouldReturnParentName() {
            when(bodyPartMapper.selectById(2L)).thenReturn(foreheadEntity);
            when(bodyPartMapper.selectById(1L)).thenReturn(headEntity);
            BodyPartDetailVO result = bodyPartService.getDetailById(2L);
            assertNotNull(result);
            assertEquals("头部", result.getParentName());
        }
    }

    // ==================== createBodyPart 测试 ====================

    @Nested
    @DisplayName("createBodyPart 测试")
    class CreateBodyPartTests {

        @Test
        @DisplayName("createBodyPart: 创建成功")
        void createBodyPart_shouldSuccess() {
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(codeGeneratorService.generate("BODYPART_NO")).thenReturn("BP-0001");
            doAnswer(invocation -> {
                BodyPartEntity entity = invocation.getArgument(0);
                entity.setId(10L);
                return 1;
            }).when(bodyPartMapper).insert(any(BodyPartEntity.class));

            bodyPartService.createBodyPart(createDTO);

            verify(bodyPartMapper, times(1)).insert(any(BodyPartEntity.class));
        }

        @Test
        @DisplayName("createBodyPart: 名称已存在时抛出异常")
        void createBodyPart_whenNameExists_shouldThrowException() {
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.createBodyPart(createDTO));
            assertEquals(ErrorCodeEnum.BODY_PART_NAME_EXISTS.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("createBodyPart: 创建二级部位时自动设置level=2")
        void createBodyPart_whenParentIdNotZero_shouldSetLevel2() {
            createDTO.setParentId(1L);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            doAnswer(invocation -> {
                BodyPartEntity entity = invocation.getArgument(0);
                assertEquals(2, entity.getLevel());
                entity.setId(10L);
                return 1;
            }).when(bodyPartMapper).insert(any(BodyPartEntity.class));

            bodyPartService.createBodyPart(createDTO);

            verify(bodyPartMapper, times(1)).insert(any(BodyPartEntity.class));
        }
    }

    // ==================== updateBodyPart 测试 ====================

    @Nested
    @DisplayName("updateBodyPart 测试")
    class UpdateBodyPartTests {

        @Test
        @DisplayName("updateBodyPart: 更新成功")
        void updateBodyPart_shouldSuccess() {
            when(bodyPartMapper.selectById(1L)).thenReturn(headEntity);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            bodyPartService.updateBodyPart(1L, updateDTO);

            verify(bodyPartMapper, times(1)).updateById(any(BodyPartEntity.class));
        }

        @Test
        @DisplayName("updateBodyPart: 数据不存在时抛出异常")
        void updateBodyPart_whenNotExists_shouldThrowException() {
            when(bodyPartMapper.selectById(999L)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.updateBodyPart(999L, updateDTO));
            assertEquals(ErrorCodeEnum.BODY_PART_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("updateBodyPart: 名称已存在时抛出异常")
        void updateBodyPart_whenNameExists_shouldThrowException() {
            when(bodyPartMapper.selectById(1L)).thenReturn(headEntity);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.updateBodyPart(1L, updateDTO));
            assertEquals(ErrorCodeEnum.BODY_PART_NAME_EXISTS.getCode(), ex.getCode());
        }
    }

    // ==================== removeBodyPart 测试 ====================

    @Nested
    @DisplayName("removeBodyPart 测试")
    class RemoveBodyPartTests {

        @Test
        @DisplayName("removeBodyPart: 删除成功")
        void removeBodyPart_shouldSuccess() {
            when(bodyPartMapper.selectById(2L)).thenReturn(foreheadEntity);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            doAnswer(invocation -> 1).when(bodyPartMapper).deleteById(2L);

            bodyPartService.removeBodyPart(2L);

            verify(bodyPartMapper, times(1)).deleteById(2L);
        }

        @Test
        @DisplayName("removeBodyPart: 数据不存在时抛出异常")
        void removeBodyPart_whenNotExists_shouldThrowException() {
            when(bodyPartMapper.selectById(999L)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.removeBodyPart(999L));
            assertEquals(ErrorCodeEnum.BODY_PART_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("removeBodyPart: 存在子节点时抛出异常")
        void removeBodyPart_whenHasChildren_shouldThrowException() {
            when(bodyPartMapper.selectById(1L)).thenReturn(headEntity);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.removeBodyPart(1L));
            assertEquals(ErrorCodeEnum.DATA_HAS_CHILDREN.getCode(), ex.getCode());
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("updateStatus 测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus: 修改状态成功")
        void updateStatus_shouldSuccess() {
            when(bodyPartMapper.selectById(1L)).thenReturn(headEntity);
            // BaseMapper#updateById 返回受影响行数 Integer，不能 stub 为 Boolean
            doAnswer(invocation -> 1).when(bodyPartMapper).updateById(any(BodyPartEntity.class));

            bodyPartService.updateStatus(1L, StatusConstants.DISABLED);

            verify(bodyPartMapper, times(1)).updateById(any(BodyPartEntity.class));
        }

        @Test
        @DisplayName("updateStatus: 状态值不合法时抛出异常")
        void updateStatus_whenInvalidStatus_shouldThrowException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.updateStatus(1L, 99));
            assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("updateStatus: 数据不存在时抛出异常")
        void updateStatus_whenNotExists_shouldThrowException() {
            when(bodyPartMapper.selectById(999L)).thenReturn(null);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.updateStatus(999L, 0));
            assertEquals(ErrorCodeEnum.BODY_PART_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ==================== listTree 测试 ====================

    @Nested
    @DisplayName("listTree 测试")
    class ListTreeTests {

        @Test
        @DisplayName("listTree: 返回树形结构")
        void listTree_shouldReturnTreeStructure() {
            List<BodyPartEntity> entities = new ArrayList<>();
            entities.add(headEntity);
            entities.add(foreheadEntity);
            when(bodyPartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(entities);

            List<BodyPartVO> result = bodyPartService.listTree();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("头部", result.get(0).getName());
            assertNotNull(result.get(0).getChildren());
            assertEquals(1, result.get(0).getChildren().size());
            assertEquals("前额", result.get(0).getChildren().get(0).getName());
        }

        @Test
        @DisplayName("listTree: 空数据时返回空列表")
        void listTree_whenEmpty_shouldReturnEmptyList() {
            when(bodyPartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            List<BodyPartVO> result = bodyPartService.listTree();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== listOptions 测试 ====================

    @Nested
    @DisplayName("listOptions 测试")
    class ListOptionsTests {

        @Test
        @DisplayName("listOptions: 返回下拉选项树")
        void listOptions_shouldReturnOptionTree() {
            List<BodyPartEntity> entities = new ArrayList<>();
            entities.add(headEntity);
            entities.add(foreheadEntity);
            when(bodyPartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(entities);

            List<BodyPartOptionVO> result = bodyPartService.listOptions();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("头部", result.get(0).getName());
            assertNotNull(result.get(0).getChildren());
        }
    }
}
