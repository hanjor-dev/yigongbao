package com.yigongbao.module.basic.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.service.ProductSpecService;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
import com.yigongbao.module.basic.product.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 产品 Service 单元测试
 *
 * @author hanjor
 * @date 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("ProductService 单元测试")
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSpecService productSpecService;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductEntity testEntity;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = productService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productService, productMapper);

        testEntity = new ProductEntity();
        testEntity.setId(1L);
        testEntity.setProductName("膝关节假体");
        testEntity.setCategory("17.1");
        testEntity.setCategoryName("下肢关节");
        testEntity.setStatus(1);
        testEntity.setCreateTime(LocalDateTime.now());
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTest {

        @Test
        @DisplayName("产品存在时返回VO（含空specs）")
        void getById_whenExists_shouldReturnVO() {
            when(productMapper.selectById(1L)).thenReturn(testEntity);
            when(productSpecService.listByProductId(1L)).thenReturn(Collections.emptyList());

            ProductVO vo = productService.getById(1L);

            assertNotNull(vo);
            assertEquals(1L, vo.getId());
            assertEquals("膝关节假体", vo.getProductName());
            assertNotNull(vo.getSpecs());
        }

        @Test
        @DisplayName("产品不存在时抛出 PRODUCT_NOT_FOUND")
        void getById_whenNotExists_shouldThrowException() {
            when(productMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productService.getById(999L)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 测试")
    class CreateTest {

        @Test
        @DisplayName("创建成功")
        void create_shouldSuccess() {
            CreateProductDTO dto = new CreateProductDTO();
            dto.setProductName("髋关节假体");
            dto.setCategory("17.1");

            when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            productService.create(dto);

            verify(productMapper, times(1)).insert(any(ProductEntity.class));
        }

        @Test
        @DisplayName("名称已存在时抛出 PRODUCT_EXISTS")
        void create_whenNameExists_shouldThrowException() {
            CreateProductDTO dto = new CreateProductDTO();
            dto.setProductName("膝关节假体");

            when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productService.create(dto)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_EXISTS.getCode(), exception.getCode());
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 测试")
    class UpdateTest {

        @Test
        @DisplayName("更新成功")
        void update_shouldSuccess() {
            UpdateProductDTO dto = new UpdateProductDTO();
            dto.setProductName("更新后的产品");

            when(productMapper.selectById(1L)).thenReturn(testEntity);

            productService.update(1L, dto);

            verify(productMapper, times(1)).updateById(any(ProductEntity.class));
        }

        @Test
        @DisplayName("产品不存在时抛出 PRODUCT_NOT_FOUND")
        void update_whenNotExists_shouldThrowException() {
            UpdateProductDTO dto = new UpdateProductDTO();
            dto.setProductName("更新后的产品");

            when(productMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productService.update(999L, dto)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== remove 测试 ====================

    @Nested
    @DisplayName("remove 测试")
    class RemoveTest {

        @Test
        @DisplayName("无规格时删除成功")
        void remove_whenNoSpecs_shouldSuccess() {
            when(productMapper.selectById(1L)).thenReturn(testEntity);
            when(productSpecService.existsByProductId(1L)).thenReturn(false);

            productService.remove(1L);

            verify(productMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("有规格时抛出 PRODUCT_HAS_SPECS")
        void remove_whenHasSpecs_shouldThrowException() {
            when(productMapper.selectById(1L)).thenReturn(testEntity);
            when(productSpecService.existsByProductId(1L)).thenReturn(true);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productService.remove(1L)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_HAS_SPECS.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("产品不存在时抛出 PRODUCT_NOT_FOUND")
        void remove_whenNotExists_shouldThrowException() {
            when(productMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> productService.remove(999L)
            );
            assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(), exception.getCode());
        }
    }

    // ==================== listProducts 分页测试 ====================

    @Nested
    @DisplayName("listProducts 分页测试")
    class ListProductsTest {

        @Test
        @DisplayName("分页查询返回数据")
        void listProducts_shouldReturnPageData() {
            Page<ProductEntity> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(testEntity));
            page.setTotal(1);
            when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            IPage<ProductVO> result = productService.listProducts(1, 10, null, null, null, null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("无数据时返回空分页")
        void listProducts_whenEmpty_shouldReturnEmptyPage() {
            Page<ProductEntity> page = new Page<>(1, 10);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            when(productMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            IPage<ProductVO> result = productService.listProducts(1, 10, null, null, null, null);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    // ==================== listAll 测试 ====================

    @Nested
    @DisplayName("listAll 测试")
    class ListAllTest {

        @Test
        @DisplayName("返回所有产品")
        void listAll_shouldReturnAll() {
            when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(testEntity));

            List<ProductVO> list = productService.listAll(null, null, null);

            assertNotNull(list);
            assertEquals(1, list.size());
        }

        @Test
        @DisplayName("无数据时返回空列表")
        void listAll_whenEmpty_shouldReturnEmptyList() {
            when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<ProductVO> list = productService.listAll(null, null, null);

            assertNotNull(list);
            assertTrue(list.isEmpty());
        }
    }

    // ==================== listByCategory 测试 ====================

    @Nested
    @DisplayName("listByCategory 测试")
    class ListByCategoryTest {

        @Test
        @DisplayName("按大类查询返回正确产品")
        void listByCategory_shouldReturnList() {
            when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(testEntity));

            List<ProductVO> list = productService.listByCategory("17.1");

            assertNotNull(list);
            assertEquals(1, list.size());
            assertEquals("17.1", list.get(0).getCategory());
        }

        @Test
        @DisplayName("无数据时返回空列表")
        void listByCategory_whenEmpty_shouldReturnEmptyList() {
            when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<ProductVO> list = productService.listByCategory("17.1");

            assertNotNull(list);
            assertTrue(list.isEmpty());
        }
    }
}
