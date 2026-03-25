package com.yigongbao.module.basic.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.vo.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BasicTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductController 接口测试")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductVO buildTestVO(Long id, String name) {
        ProductVO vo = new ProductVO();
        vo.setId(id);
        vo.setProductCode("P-" + String.format("%04d", id));
        vo.setProductName(name);
        vo.setCategory("关节");
        vo.setCertId(1L);
        vo.setCertCode("REG-001");
        vo.setPrice(new BigDecimal("50000.00"));
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== page 测试 ====================

    @Nested
    @DisplayName("page 测试")
    class PageTests {

        @Test
        @DisplayName("page: 分页查询成功")
        void page_shouldReturnPageData() throws Exception {
            ProductVO vo = buildTestVO(1L, "膝关节假体");
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductVO>(1, 10);
            page.setRecords(List.of(vo));
            page.setTotal(1);
            when(productService.listProducts(eq(1), eq(10), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/basic/product/page"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.records[0].productName").value("膝关节假体"));
        }
    }

    // ==================== list 测试 ====================

    @Nested
    @DisplayName("list 测试")
    class ListTests {

        @Test
        @DisplayName("list: 返回所有产品")
        void list_shouldReturnAll() throws Exception {
            when(productService.listAll(any(), any(), any()))
                    .thenReturn(List.of(buildTestVO(1L, "膝关节假体"), buildTestVO(2L, "髋关节假体")));

            mockMvc.perform(get("/api/basic/product/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].productName").value("膝关节假体"));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 存在时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            when(productService.getById(1L)).thenReturn(buildTestVO(1L, "膝关节假体"));

            mockMvc.perform(get("/api/basic/product/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.productName").value("膝关节假体"));
        }

        @Test
        @DisplayName("getById: 不存在时返回错误")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(productService.getById(999L))
                    .thenThrow(new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND));

            mockMvc.perform(get("/api/basic/product/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(710))
                    .andExpect(jsonPath("$.message").value("产品型号不存在"));
        }
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 测试")
    class CreateTests {

        @Test
        @DisplayName("create: 创建成功返回200")
        void create_shouldSuccess() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "productName", "测试产品",
                    "category", "关节"
            );

            mockMvc.perform(post("/api/basic/product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("create: 缺少必填参数时返回400")
        void create_whenMissingRequiredParam_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "category", "关节"
            );

            mockMvc.perform(post("/api/basic/product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 测试")
    class UpdateTests {

        @Test
        @DisplayName("update: 更新成功返回200")
        void update_shouldSuccess() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "productName", "更新后的产品名称"
            );

            mockMvc.perform(put("/api/basic/product/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("update: 数据不存在时返回错误")
        void update_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND))
                    .when(productService).update(eq(999L), any());

            Map<String, Object> requestBody = Map.of(
                    "productName", "更新后的产品名称"
            );

            mockMvc.perform(put("/api/basic/product/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(710))
                    .andExpect(jsonPath("$.message").value("产品型号不存在"));
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("delete: 删除成功返回200")
        void delete_shouldSuccess() throws Exception {
            mockMvc.perform(delete("/api/basic/product/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("delete: 数据不存在时返回错误")
        void delete_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND))
                    .when(productService).remove(999L);

            mockMvc.perform(delete("/api/basic/product/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(710));
        }
    }

    // ==================== listByCert 测试 ====================

    @Nested
    @DisplayName("listByCert 测试")
    class ListByCertTests {

        @Test
        @DisplayName("listByCert: 按注册证查询成功")
        void listByCert_shouldReturnList() throws Exception {
            when(productService.listByCertId(1L))
                    .thenReturn(List.of(buildTestVO(1L, "膝关节假体")));

            mockMvc.perform(get("/api/basic/product/list-by-cert/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].productName").value("膝关节假体"));
        }
    }

    // ==================== listByCategory 测试 ====================

    @Nested
    @DisplayName("listByCategory 测试")
    class ListByCategoryTests {

        @Test
        @DisplayName("listByCategory: 按分类查询成功")
        void listByCategory_shouldReturnList() throws Exception {
            when(productService.listByCategory("关节"))
                    .thenReturn(List.of(buildTestVO(1L, "膝关节假体")));

            mockMvc.perform(get("/api/basic/product/list-by-category")
                            .param("category", "关节"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].productName").value("膝关节假体"));
        }
    }
}
