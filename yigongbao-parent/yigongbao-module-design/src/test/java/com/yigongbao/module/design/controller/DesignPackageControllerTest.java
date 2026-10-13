package com.yigongbao.module.design.controller;

import com.yigongbao.module.design.service.DesignFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignPackageController.class)
class DesignPackageControllerTest {

    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @MockBean private DesignFileService designFileService;

    @Test
    void uploadPackage_requiresOrderIdAndFile() throws Exception {
        mockMvc.perform(multipart("/design/package/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadPackage_delegatesMultipartRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.zip", "application/zip", new byte[]{1});

        mockMvc.perform(multipart("/design/package/upload")
                        .file(file)
                        .param("orderId", "7"))
                .andExpect(status().isOk());

        verify(designFileService).uploadPackage(eq(7L), any());
    }

    @Test
    void deletePackage_passesOrderAndPackageIds() throws Exception {
        mockMvc.perform(delete("/design/package/{packageId}", 9L).param("orderId", "7"))
                .andExpect(status().isOk());

        verify(designFileService).deletePackage(7L, 9L);
    }

    @Test
    void listPackageFiles_passesOrderAndPackageIds() throws Exception {
        mockMvc.perform(get("/design/package/{packageId}/files", 9L).param("orderId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(designFileService).listPackageFiles(7L, 9L);
    }

    @Test
    void listPackages_passesOrderId() throws Exception {
        mockMvc.perform(get("/design/packages").param("orderId", "7"))
                .andExpect(status().isOk());

        verify(designFileService).listPackages(7L);
    }
}
