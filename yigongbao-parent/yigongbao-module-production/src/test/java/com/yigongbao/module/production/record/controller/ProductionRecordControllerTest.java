package com.yigongbao.module.production.record.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionRecordController.class)
class ProductionRecordControllerTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionRecordService recordService;

    @Test
    void assignDevice_rejectsMissingDeviceId() throws Exception {
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordService);
    }

    @Test
    void assignDevice_delegatesValidatedRequest() throws Exception {
        AssignDeviceDTO dto = new AssignDeviceDTO();
        dto.setDeviceId(8L);
        dto.setMaterial("Ti");

        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(recordService).assignDevice(7L, dto);
    }

    @Test
    void submitBatchNo_rejectsBlankBatchNo() throws Exception {
        mockMvc.perform(post("/production/record/{id}/submit-batch-no", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productionBatchNo\":\" \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordService);
    }

    @Test
    void generateBatchNo_returnsServiceValue() throws Exception {
        when(recordService.generateBatchNo(7L)).thenReturn("BATCH-7");

        mockMvc.perform(get("/production/record/{id}/generate-batch-no", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("BATCH-7"));

        verify(recordService).generateBatchNo(7L);
    }

    @Test
    void downloadDataPackage_delegatesRecordId() throws Exception {
        when(recordService.downloadDataPackage(7L)).thenReturn("https://file/package.zip");
        mockMvc.perform(post("/production/record/{id}/download-package", 7L))
                .andExpect(status().isOk());
        verify(recordService).downloadDataPackage(7L);
    }

    @Test
    void getDeviceConfig_delegatesRecordId() throws Exception {
        mockMvc.perform(get("/production/record/{id}/device-config", 7L))
                .andExpect(status().isOk());
        verify(recordService).getDeviceConfig(7L);
    }

    @Test
    void listPrinters_delegatesService() throws Exception {
        mockMvc.perform(get("/production/record/printers"))
                .andExpect(status().isOk());
        verify(recordService).listPrinters();
    }

    @Test
    void cancelPreview_delegatesRecordId() throws Exception {
        mockMvc.perform(get("/production/record/{id}/cancel-preview", 7L))
                .andExpect(status().isOk());
        verify(recordService).getCancelPreview(7L);
    }

    @Test
    void listAndDetail_delegateRecordQueries() throws Exception {
        mockMvc.perform(post("/production/record/list")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(recordService).pageRecords(any());

        mockMvc.perform(get("/production/record/{id}", 7L))
                .andExpect(status().isOk());
        verify(recordService).getRecordDetail(7L);
    }

    @Test
    void submitBatchNo_delegatesValidBatchNumber() throws Exception {
        SubmitBatchNoDTO dto = new SubmitBatchNoDTO();
        dto.setProductionBatchNo("BATCH-7");
        mockMvc.perform(post("/production/record/{id}/submit-batch-no", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(recordService).submitBatchNo(7L, dto);
    }

    @Test
    void flowCardExcel_delegatesRecordId() throws Exception {
        mockMvc.perform(get("/production/record/{id}/excel", 7L))
                .andExpect(status().isOk());
        verify(recordService).getOrGenerateFlowCardExcel(7L);
    }

    @Test
    void productLedgerExport_delegatesQuery() throws Exception {
        when(recordService.exportProductLedger(any())).thenReturn(new byte[]{1, 2});
        mockMvc.perform(post("/production/record/product-ledger/export")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(recordService).exportProductLedger(any());
    }
}
