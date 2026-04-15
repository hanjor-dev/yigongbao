package com.yigongbao.module.basic.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.service.SpecReferenceChecker;
import com.yigongbao.module.basic.product.dto.CreateProductSpecDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductSpecDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.entity.ProductSpecEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.mapper.ProductSpecMapper;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 产品规格 Service 单元测试
 *
 * @author hanjor
 * @date 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductSpecService 单元测试")
class ProductSpecServiceImplTest {

    @Mock
    private ProductSpecMapper productSpecMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SpecReferenceChecker specReferenceChecker;

    @InjectMocks
    private ProductSpecServiceImpl productSpecService;

    private ProductEntity testProduct;
    private ProductSpecEntity testSpec;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = productSpecService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productSpecService, productSpecMapper);

        // 注入 specReferenceChecker（@Autowired(required=false) 不会被 @InjectMocks 自动注入）
        Field checkerField = ProductSpecServiceImpl.class.getDeclaredField("specReferenceChecker");
        checkerField.setAccessible(true);
        checkerField.set(productSpecService, specReferenceChecker);

        testProduct = new ProductEntity();
        testProduct.setId(1L);
        testProduct.setProductName("膝关节假体");
        testProduct.setStatus(1);

        testSpec = new ProductSpecEntity();
        testSpec.setId(10L);
        testSpec.setProductId(1L);
        testSpec.setSpecName("47号");
        testSpec.setCertNo("国械注准20250001");
        testSpec.setStatus(1);
        testSpec.setSort(0);
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 测试")
    class CreateTest {

        @Test
        @DisplayName("创建规格成功（自动关联注册证号）")
        void create_shouldSuccess() {
            CreateProductSpecDTO dto = new CreateProductSpecDTO();
            dto.setSpecName("47号");
            dto.setCertId(100L);
            dto.setCertNo("国械注准20250001");

            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productSpecMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            productSpecService.create(1L, dto);

            verify(productSpecMapper, times(1)).insert(any(ProductSpecEntity.class));
        }

        @Test
        @DisplayName("产品不存在时抛出 PRODUCT_NOT_FOUND")
        void create_whenProductNotFound_shouldThrow() {
            CreateProductSpecDTO dto = new CreateProductSpecDTO();
            dto.setSpecName("47号");

            when(productMapper.selectById(99L)).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productSpecService.create(99L, dto)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("同产品下规格名重复时抛出 PRODUCT_SPEC_EXISTS")
        void create_whenSpecNameExists_shouldThrow() {
            CreateProductSpecDTO dto = new CreateProductSpecDTO();
            dto.setSpecName("47号");

            when(productMapper.selectById(1L)).thenReturn(testProduct);
            when(productSpecMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productSpecService.create(1L, dto)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_SPEC_EXISTS.getCode(), exception.getCode());
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 测试")
    class UpdateTest {

        @Test
        @DisplayName("更新规格成功")
        void update_shouldSuccess() {
            UpdateProductSpecDTO dto = new UpdateProductSpecDTO();
            dto.setSpecName("48号");

            when(productSpecMapper.selectById(10L)).thenReturn(testSpec);
            // 新名称不重复
            when(productSpecMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            productSpecService.update(10L, dto);

            verify(productSpecMapper, times(1)).updateById(any(ProductSpecEntity.class));
        }

        @Test
        @DisplayName("规格不存在时抛出 PRODUCT_SPEC_NOT_FOUND")
        void update_whenNotFound_shouldThrow() {
            UpdateProductSpecDTO dto = new UpdateProductSpecDTO();
            dto.setSpecName("48号");

            when(productSpecMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productSpecService.update(999L, dto)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_SPEC_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== remove 测试 ====================

    @Nested
    @DisplayName("remove 测试")
    class RemoveTest {

        @Test
        @DisplayName("未被引用时删除成功")
        void remove_whenNotInUse_shouldSuccess() {
            when(productSpecMapper.selectById(10L)).thenReturn(testSpec);
            when(specReferenceChecker.isSpecInUse(10L)).thenReturn(false);

            productSpecService.remove(10L);

            verify(productSpecMapper, times(1)).deleteById(10L);
        }

        @Test
        @DisplayName("规格不存在时抛出 PRODUCT_SPEC_NOT_FOUND")
        void remove_whenNotFound_shouldThrow() {
            when(productSpecMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productSpecService.remove(999L)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_SPEC_NOT_FOUND.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("规格已被打印信息引用时抛出 PRODUCT_SPEC_IN_USE")
        void remove_whenInUse_shouldThrow() {
            when(productSpecMapper.selectById(10L)).thenReturn(testSpec);
            when(specReferenceChecker.isSpecInUse(10L)).thenReturn(true);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productSpecService.remove(10L)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_SPEC_IN_USE.getCode(), exception.getCode());
        }
    }

    // ==================== listByProductId 测试 ====================

    @Nested
    @DisplayName("listByProductId 测试")
    class ListByProductIdTest {

        @Test
        @DisplayName("返回规格列表")
        void listByProductId_shouldReturnList() {
            when(productSpecMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(testSpec));

            List<ProductSpecVO> list = productSpecService.listByProductId(1L);

            assertNotNull(list);
            assertEquals(1, list.size());
            assertEquals("47号", list.get(0).getSpecName());
        }

        @Test
        @DisplayName("无规格时返回空列表")
        void listByProductId_whenEmpty_shouldReturnEmpty() {
            when(productSpecMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<ProductSpecVO> list = productSpecService.listByProductId(1L);

            assertNotNull(list);
            assertTrue(list.isEmpty());
        }
    }
}
