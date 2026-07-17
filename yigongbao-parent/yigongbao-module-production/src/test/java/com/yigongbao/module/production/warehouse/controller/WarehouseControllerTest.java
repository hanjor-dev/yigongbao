package com.yigongbao.module.production.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;
import com.yigongbao.module.production.warehouse.service.IWarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {
    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IWarehouseService warehouseService;

    @Test
    void warehouseInProduct_delegatesProductAndRemark() throws Exception {
        WarehouseInProductDTO dto = new WarehouseInProductDTO();
        dto.setRemark("入库");
        mockMvc.perform(post("/production/warehouse/in/products/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(warehouseService).warehouseInProduct(9L, dto);
    }

    @Test
    void warehouseOutProduct_delegatesProductAndRemark() throws Exception {
        WarehouseOutProductDTO dto = new WarehouseOutProductDTO();
        dto.setRemark("出库");
        mockMvc.perform(post("/production/warehouse/out/products/{id}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(warehouseService).warehouseOutProduct(9L, dto);
    }

    @Test
    void warehouseQueries_delegateRequestAndRecordIds() throws Exception {
        mockMvc.perform(post("/production/warehouse/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(warehouseService).listWarehouse(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(get("/production/warehouse/detail/{id}", 9L))
                .andExpect(status().isOk());
        verify(warehouseService).getWarehouseDetail(9L);

        mockMvc.perform(post("/production/warehouse/products/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(warehouseService).listWarehouseProducts(org.mockito.ArgumentMatchers.any());
    }
}
