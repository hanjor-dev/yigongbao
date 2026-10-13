package com.yigongbao.module.production.qc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.qc.dto.BatchUpdateUdiDTO;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionQcController.class)
class ProductionQcControllerTest {

    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionQcService qcService;

    @Test
    void markProductFail_requiresReason() throws Exception {
        mockMvc.perform(post("/production/qc/product/{id}/fail", 3L))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(qcService);
    }

    @Test
    void markProductPass_delegatesId() throws Exception {
        mockMvc.perform(post("/production/qc/product/{id}/pass", 3L))
                .andExpect(status().isOk());
        verify(qcService).markProductPass(3L);
    }

    @Test
    void batchUpdateUdi_delegatesValidatedDto() throws Exception {
        BatchUpdateUdiDTO dto = new BatchUpdateUdiDTO();
        dto.setRecordId(7L);
        BatchUpdateUdiDTO.ProductUdiItem item = new BatchUpdateUdiDTO.ProductUdiItem();
        item.setProductId(8L);
        item.setUdiCode("UDI-8");
        dto.setProducts(java.util.List.of(item));

        mockMvc.perform(post("/production/qc/batch-update-udi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(qcService).batchUpdateUdi(any(BatchUpdateUdiDTO.class));
    }

    @Test
    void listAndProducts_delegateQueries() throws Exception {
        mockMvc.perform(post("/production/qc/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(qcService).listQcRecords(any());

        mockMvc.perform(get("/production/qc/{id}/products", 7L))
                .andExpect(status().isOk());
        verify(qcService).listProductsByRecordId(7L);
    }

    @Test
    void failAndTransfer_delegateIdsAndReason() throws Exception {
        mockMvc.perform(post("/production/qc/product/{id}/fail", 3L)
                        .param("reason", "尺寸不合格"))
                .andExpect(status().isOk());
        verify(qcService).markProductFail(3L, "尺寸不合格");

        mockMvc.perform(post("/production/qc/{id}/transfer-to-pack", 7L))
                .andExpect(status().isOk());
        verify(qcService).transferToPacking(7L);
    }
}
