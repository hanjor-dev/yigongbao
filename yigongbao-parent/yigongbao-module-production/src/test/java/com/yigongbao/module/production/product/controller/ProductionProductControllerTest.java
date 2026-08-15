package com.yigongbao.module.production.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.production.product.dto.ProductionProductPageDTO;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.module.production.product.vo.ProductionProductDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionProductController.class)
class ProductionProductControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionProductService productService;

    @Test
    void list_delegatesPageQuery() throws Exception {
        ProductionProductPageDTO dto = new ProductionProductPageDTO();
        mockMvc.perform(post("/production/product/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(productService).pageProductDetails(dto);
    }

    @Test
    void list_serializesPopulatedPrintDeviceCode() throws Exception {
        ProductionProductDetailVO product = new ProductionProductDetailVO();
        objectMapper.readerForUpdating(product)
                .readValue("{\"printDeviceCode\":\"PRINTER-01\"}");
        Page<ProductionProductDetailVO> page = new Page<>(1, 10);
        page.setRecords(List.of(product));
        when(productService.pageProductDetails(any())).thenReturn(page);

        mockMvc.perform(post("/production/product/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].printDeviceCode").value("PRINTER-01"));
    }

    @Test
    void list_serializesExplicitNullPrintDeviceCode() throws Exception {
        Page<ProductionProductDetailVO> page = new Page<>(1, 10);
        page.setRecords(List.of(new ProductionProductDetailVO()));
        when(productService.pageProductDetails(any())).thenReturn(page);

        mockMvc.perform(post("/production/product/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].printDeviceCode").value(nullValue()));
    }
}
