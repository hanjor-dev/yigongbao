package com.yigongbao.module.production.record.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.AssignProductWeightDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.service.ProductionPrintLifecycleService;
import com.yigongbao.module.production.record.vo.DeviceConfigVO;
import com.yigongbao.module.production.record.vo.PrinterOccupationVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ProductionRecordController.class,
        properties = "spring.jackson.default-property-inclusion=non_null")
class ProductionRecordControllerTest {

    @SpringBootApplication
    static class TestApplication {
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IProductionRecordService recordService;
    @MockBean private ProductionPrintLifecycleService printLifecycleService;

    @Test
    void printerOccupation_returnsOccupiedStatusAndBindsParameters() throws Exception {
        when(recordService.getPrinterOccupation(7L, 8L))
                .thenReturn(new PrinterOccupationVO(true));

        mockMvc.perform(get("/production/record/{recordId}/printer-occupation", 7L)
                        .param("deviceId", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occupied").value(true));

        verify(recordService).getPrinterOccupation(7L, 8L);
    }

    @Test
    void printerOccupation_rejectsMissingDeviceIdWithoutCallingService() throws Exception {
        mockMvc.perform(get("/production/record/{recordId}/printer-occupation", 7L))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordService);
    }

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
        AssignProductWeightDTO productWeight = new AssignProductWeightDTO();
        productWeight.setProductId(101L);
        productWeight.setWeight(new java.math.BigDecimal("12.35"));
        dto.setProductWeights(java.util.List.of(productWeight));

        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(recordService).assignDevice(7L, dto);
    }

    @Test
    void releaseDevice_delegatesRecordId() throws Exception {
        mockMvc.perform(post("/production/record/{id}/release-device", 7L))
                .andExpect(status().isOk());

        verify(recordService).releaseDevice(7L);
    }

    @Test
    void releaseDevice_usesCancelOperationLog() throws Exception {
        OperationLog operationLog = ProductionRecordController.class
                .getDeclaredMethod("releaseDevice", Long.class)
                .getAnnotation(OperationLog.class);

        assertThat(operationLog).isNotNull();
        assertThat(operationLog.businessType()).isEqualTo(OperationTypeEnum.CANCEL);
        assertThat(operationLog.operation()).isEqualTo("强制释放打印设备配置");
    }

    @Test
    void forceCompletePrint_delegatesRecordId() throws Exception {
        mockMvc.perform(post("/production/record/{id}/force-complete-print", 7L))
                .andExpect(status().isOk());

        verify(printLifecycleService).forceCompletePrint(7L);
    }

    @Test
    void forceCompletePrint_requiresPermissionAndOperationLog() throws Exception {
        var method = ProductionRecordController.class
                .getDeclaredMethod("forceCompletePrint", Long.class);
        var permission = method.getAnnotation(com.yigongbao.framework.annotation.RequirePermission.class);
        var operationLog = method.getAnnotation(OperationLog.class);

        assertThat(permission).isNotNull();
        assertThat(permission.value()).isEqualTo("manufacture:ForceCompletePrint");
        assertThat(operationLog).isNotNull();
        assertThat(operationLog.businessType()).isEqualTo(OperationTypeEnum.UPDATE);
        assertThat(operationLog.operation()).isEqualTo("强制完成打印");
    }

    @Test
    void assignDevice_requestModelIncludesProductWeights() throws Exception {
        assertThat(AssignDeviceDTO.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("productWeights"));
    }

    @Test
    void assignDevice_requestModelIncludesOccupiedConfirmation() {
        assertThat(AssignDeviceDTO.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("confirmOccupied")
                        && field.getType().equals(Boolean.class));
    }

    @Test
    void assignDevice_bindsProductWeightsFromJson() throws Exception {
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"productWeights\":[{\"productId\":101,\"weight\":12.35},{\"productId\":102}]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AssignDeviceDTO> captor = ArgumentCaptor.forClass(AssignDeviceDTO.class);
        verify(recordService).assignDevice(eq(7L), captor.capture());
        assertThat(captor.getValue().getProductWeights()).hasSize(2);
        assertThat(captor.getValue().getProductWeights().get(0).getWeight())
                .isEqualByComparingTo("12.35");
        assertThat(captor.getValue().getProductWeights().get(1).getWeight()).isNull();
    }

    @Test
    void assignDevice_bindsOmittedFalseAndTrueOccupiedConfirmation() throws Exception {
        String weights = "\"productWeights\":[{\"productId\":101,\"weight\":12.35}]";
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8," + weights + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"confirmOccupied\":false," + weights + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"confirmOccupied\":true," + weights + "}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AssignDeviceDTO> captor = ArgumentCaptor.forClass(AssignDeviceDTO.class);
        verify(recordService, org.mockito.Mockito.times(3)).assignDevice(eq(7L), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AssignDeviceDTO::getConfirmOccupied)
                .containsExactly(null, false, true);
    }

    @Test
    void assignDevice_rejectsNegativeProductWeight() throws Exception {
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"productWeights\":[{\"productId\":101,\"weight\":-1}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordService);
    }

    @Test
    void assignDevice_rejectsMissingOrEmptyProductWeights() throws Exception {
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"productWeights\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordService);
    }

    @Test
    void assignDevice_rejectsInvalidProductWeightScaleAndProductId() throws Exception {
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"productWeights\":[{\"weight\":1.00}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"productWeights\":[{\"productId\":101,\"weight\":1.234}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/production/record/{id}/assign-device", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":8,\"productWeights\":[{\"productId\":101,\"weight\":999999999.99}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recordService);
    }

    @Test
    void submitBatchNo_acceptsLegacyBlankBatchNoAndDelegatesMaterialBatch() throws Exception {
        mockMvc.perform(post("/production/record/{id}/submit-batch-no", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productionBatchNo\":\" \"}"))
                .andExpect(status().isOk());

        verify(recordService).submitBatchNo(eq(7L), any(SubmitBatchNoDTO.class));
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
    void getDeviceConfig_returnsPrintSettings() throws Exception {
        DeviceConfigVO config = new DeviceConfigVO();
        config.setMaterial("光敏树脂");
        config.setPrintParams("{\"layerHeight\":0.05}");
        when(recordService.getDeviceConfig(7L)).thenReturn(config);

        mockMvc.perform(get("/production/record/{id}/device-config", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.material").value("光敏树脂"))
                .andExpect(jsonPath("$.data.printParams").value("{\"layerHeight\":0.05}"));
    }

    @Test
    void getDeviceConfig_omitsNullPrintSettings() throws Exception {
        when(recordService.getDeviceConfig(7L)).thenReturn(new DeviceConfigVO());

        mockMvc.perform(get("/production/record/{id}/device-config", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.material").doesNotExist())
                .andExpect(jsonPath("$.data.printParams").doesNotExist());
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
        verify(recordService).generateFlowCardExcel(7L);
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
