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
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
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

    private BodyPartEntity skullEntity;
    private CreateBodyPartDTO createDTO;
    private UpdateBodyPartDTO updateDTO;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(bodyPartService, bodyPartMapper);

        LocalDateTime now = LocalDateTime.now();

        skullEntity = new BodyPartEntity();
        skullEntity.setId(1L);
        skullEntity.setName("颅骨");
        skullEntity.setCode("BP-0001");
        skullEntity.setSort(1);
        skullEntity.setStatus(1);
        skullEntity.setCreateTime(now);
        skullEntity.setUpdateTime(now);

        createDTO = new CreateBodyPartDTO();
        createDTO.setName("脊柱");
        createDTO.setSort(2);

        updateDTO = new UpdateBodyPartDTO();
        updateDTO.setName("更新后的部位");
        updateDTO.setSort(3);
    }

    @Nested
    @DisplayName("listAll 测试")
    class ListAllTests {

        @Test
        @DisplayName("listAll: 返回平级列表")
        void listAll_shouldReturnFlatList() {
            List<BodyPartEntity> entities = List.of(skullEntity);
            when(bodyPartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(entities);

            List<BodyPartVO> result = bodyPartService.listAll();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("颅骨", result.get(0).getName());
        }

        @Test
        @DisplayName("listAll: 空数据时返回空列表")
        void listAll_whenEmpty_shouldReturnEmptyList() {
            when(bodyPartMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            List<BodyPartVO> result = bodyPartService.listAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getDetailById 测试")
    class GetDetailByIdTests {

        @Test
        @DisplayName("getDetailById: 存在数据时返回详情")
        void getDetailById_whenExists_shouldReturnData() {
            when(bodyPartMapper.selectById(1L)).thenReturn(skullEntity);

            BodyPartDetailVO result = bodyPartService.getDetailById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("颅骨", result.getName());
        }

        @Test
        @DisplayName("getDetailById: 数据不存在时抛出异常")
        void getDetailById_whenNotExists_shouldThrowException() {
            when(bodyPartMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.getDetailById(999L));
            assertEquals(ErrorCodeEnum.BODY_PART_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("createBodyPart 测试")
    class CreateBodyPartTests {

        @Test
        @DisplayName("createBodyPart: 创建成功")
        void createBodyPart_shouldSuccess() {
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(codeGeneratorService.generate("BODYPART_NO")).thenReturn("BP-0002");
            doAnswer(invocation -> {
                BodyPartEntity entity = invocation.getArgument(0);
                entity.setId(10L);
                return 1;
            }).when(bodyPartMapper).insert(any(BodyPartEntity.class));

            bodyPartService.createBodyPart(createDTO);

            verify(bodyPartMapper, times(1)).insert(any(BodyPartEntity.class));
        }

        @Test
        @DisplayName("createBodyPart: 名称已存在时抛出异常（全局唯一）")
        void createBodyPart_whenNameExists_shouldThrowException() {
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.createBodyPart(createDTO));
            assertEquals(ErrorCodeEnum.BODY_PART_NAME_EXISTS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateBodyPart 测试")
    class UpdateBodyPartTests {

        @Test
        @DisplayName("updateBodyPart: 更新成功")
        void updateBodyPart_shouldSuccess() {
            when(bodyPartMapper.selectById(1L)).thenReturn(skullEntity);
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
        @DisplayName("updateBodyPart: 名称已被其他记录占用时抛出异常")
        void updateBodyPart_whenNameExists_shouldThrowException() {
            when(bodyPartMapper.selectById(1L)).thenReturn(skullEntity);
            when(bodyPartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.updateBodyPart(1L, updateDTO));
            assertEquals(ErrorCodeEnum.BODY_PART_NAME_EXISTS.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("removeBodyPart 测试")
    class RemoveBodyPartTests {

        @Test
        @DisplayName("removeBodyPart: 删除成功")
        void removeBodyPart_shouldSuccess() {
            when(bodyPartMapper.selectById(1L)).thenReturn(skullEntity);
            doAnswer(invocation -> 1).when(bodyPartMapper).deleteById(1L);

            bodyPartService.removeBodyPart(1L);

            verify(bodyPartMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("removeBodyPart: 数据不存在时抛出异常")
        void removeBodyPart_whenNotExists_shouldThrowException() {
            when(bodyPartMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bodyPartService.removeBodyPart(999L));
            assertEquals(ErrorCodeEnum.BODY_PART_NOT_FOUND.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("updateStatus 测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus: 修改状态成功")
        void updateStatus_shouldSuccess() {
            when(bodyPartMapper.selectById(1L)).thenReturn(skullEntity);
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
}
