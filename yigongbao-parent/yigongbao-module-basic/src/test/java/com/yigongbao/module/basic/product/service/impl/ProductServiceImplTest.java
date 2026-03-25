package com.yigongbao.module.basic.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.entity.ProductEntity;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.vo.ProductVO;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 产品型号 Service 单元测试
 *
 * @author hanjor
 * @date 2026-03-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("ProductService 单元测试")
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private RegistrationCertService registrationCertService;

    @Mock
    private CodeGeneratorService codeGeneratorService;

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
        testEntity.setProductCode("P-001");
        testEntity.setProductName("膝关节假体");
        testEntity.setCategory("关节");
        testEntity.setCertId(1L);
        testEntity.setPrice(new BigDecimal("50000.00"));
        testEntity.setStatus(1);
        testEntity.setCreateTime(LocalDateTime.now());
    }

    // ==================== getById 测试 ====================

    @Test
    @DisplayName("getById: 产品存在时返回VO")
    void getById_whenExists_shouldReturnVO() {
        when(productMapper.selectById(1L)).thenReturn(testEntity);

        ProductVO vo = productService.getById(1L);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("膝关节假体", vo.getProductName());
    }

    @Test
    @DisplayName("getById: 产品不存在时抛出异常")
    void getById_whenNotExists_shouldThrowException() {
        when(productMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.getById(999L)
        );
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== create 测试 ====================

    @Test
    @DisplayName("create: 创建成功")
    void create_shouldSuccess() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setProductName("髋关节假体");
        dto.setCategory("关节");
        dto.setCertId(1L);

        RegistrationCertVO certVO = new RegistrationCertVO();
        certVO.setId(1L);
        certVO.setCertCode("REG-001");
        when(registrationCertService.getById(1L)).thenReturn(certVO);
        when(codeGeneratorService.generate("PRODUCT_CODE")).thenReturn("P-002");
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        productService.create(dto);

        verify(productMapper, times(1)).insert(any(ProductEntity.class));
    }

    @Test
    @DisplayName("create: 注册证不存在时抛出异常")
    void create_whenCertNotExists_shouldThrowException() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setProductName("髋关节假体");
        dto.setCertId(999L);

        when(registrationCertService.getById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.create(dto)
        );
        assertEquals(ErrorCodeEnum.CERT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("create: 编码已存在时抛出异常")
    void create_whenCodeExists_shouldThrowException() {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setProductName("髋关节假体");

        RegistrationCertVO certVO = new RegistrationCertVO();
        certVO.setId(1L);
        when(registrationCertService.getById(1L)).thenReturn(certVO);
        when(codeGeneratorService.generate("PRODUCT_CODE")).thenReturn("P-001");
        // 编码已存在
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.create(dto)
        );
        assertEquals(ErrorCodeEnum.PRODUCT_EXISTS.getCode(), exception.getCode());
    }

    // ==================== update 测试 ====================

    @Test
    @DisplayName("update: 更新成功")
    void update_shouldSuccess() {
        UpdateProductDTO dto = new UpdateProductDTO();
        dto.setProductName("更新后的产品");
        dto.setPrice(new BigDecimal("60000.00"));

        when(productMapper.selectById(1L)).thenReturn(testEntity);

        productService.update(1L, dto);

        verify(productMapper, times(1)).updateById(any(ProductEntity.class));
    }

    @Test
    @DisplayName("update: 产品不存在时抛出异常")
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

    // ==================== remove 测试 ====================

    @Test
    @DisplayName("remove: 删除成功")
    void remove_shouldSuccess() {
        when(productMapper.selectById(1L)).thenReturn(testEntity);

        productService.remove(1L);

        verify(productMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("remove: 产品不存在时抛出异常")
    void remove_whenNotExists_shouldThrowException() {
        when(productMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.remove(999L)
        );
        assertEquals(ErrorCodeEnum.PRODUCT_NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== listProducts 分页测试 ====================

    @Test
    @DisplayName("listProducts: 分页查询返回数据")
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
    @DisplayName("listProducts: 无数据时返回空分页")
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

    // ==================== listAll 测试 ====================

    @Test
    @DisplayName("listAll: 返回所有产品")
    void listAll_shouldReturnAll() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testEntity));

        List<ProductVO> list = productService.listAll(null, null, null);

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("listAll: 无数据时返回空列表")
    void listAll_whenEmpty_shouldReturnEmptyList() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ProductVO> list = productService.listAll(null, null, null);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== listByCertId 测试 ====================

    @Test
    @DisplayName("listByCertId: 返回产品列表")
    void listByCertId_shouldReturnList() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testEntity));

        List<ProductVO> list = productService.listByCertId(1L);

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("listByCertId: 无数据时返回空列表")
    void listByCertId_whenEmpty_shouldReturnEmptyList() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ProductVO> list = productService.listByCertId(1L);

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== listByCategory 测试 ====================

    @Test
    @DisplayName("listByCategory: 返回产品列表")
    void listByCategory_shouldReturnList() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(testEntity));

        List<ProductVO> list = productService.listByCategory("关节");

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("listByCategory: 无数据时返回空列表")
    void listByCategory_whenEmpty_shouldReturnEmptyList() {
        when(productMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ProductVO> list = productService.listByCategory("关节");

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
}
