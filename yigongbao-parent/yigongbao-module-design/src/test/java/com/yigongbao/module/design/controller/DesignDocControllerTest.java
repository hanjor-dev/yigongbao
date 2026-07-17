package com.yigongbao.module.design.controller;

import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DesignDocController.class)
class DesignDocControllerTest {

    @SpringBootApplication static class TestApplication {}
    @Autowired private MockMvc mockMvc;
    @MockBean private DesignDocService docService;
    @MockBean private DesignScreenshotService screenshotService;

    @Test
    void previewUrl_delegatesOrderAndPackageIds() throws Exception {
        mockMvc.perform(get("/design/workorder/{orderId}/package/{packageId}/drawing/preview-url", 1L, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(docService).getDrawingPreviewUrl(1L, 2L);
    }

    @Test
    void versions_delegatesIds() throws Exception {
        mockMvc.perform(get("/design/workorder/{orderId}/package/{packageId}/instruction/versions", 1L, 2L))
                .andExpect(status().isOk());
        verify(docService).listInstructionVersions(1L, 2L);
    }

    @Test
    void confirmDrawing_delegatesAllIds() throws Exception {
        mockMvc.perform(post("/design/workorder/{orderId}/package/{packageId}/drawing/confirm/{id}", 1L, 2L, 3L))
                .andExpect(status().isOk());
        verify(docService).confirmDrawing(1L, 2L, 3L);
    }

    @Test
    void uploadRevisedDrawing_requiresFile() throws Exception {
        mockMvc.perform(multipart("/design/workorder/{orderId}/package/{packageId}/drawing/upload-revised/{id}", 1L, 2L, 3L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveScreenshot_delegatesPackageFileAndMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "screen.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart("/design/workorder/{orderId}/package/{packageId}/files/{fileId}/screenshot", 1L, 2L, 3L)
                        .file(file))
                .andExpect(status().isOk());
        verify(screenshotService).saveScreenshot(eq(2L), eq(3L), any());
    }

    @Test
    void downloadInstruction_delegatesIds() throws Exception {
        mockMvc.perform(get("/design/workorder/1/package/2/instruction/download"))
                .andExpect(status().isOk());
        verify(docService).downloadInstruction(eq(1L), eq(2L), any());
    }

    @Test
    void uploadRevisedInstruction_delegatesMultipart() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "instruction.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        mockMvc.perform(multipart("/design/workorder/1/package/2/instruction/upload-revised/3")
                        .file(file))
                .andExpect(status().isOk());
        verify(docService).uploadRevisedInstruction(eq(1L), eq(2L), eq(3L), any());
    }

    @Test
    void confirmInstruction_delegatesAllIds() throws Exception {
        mockMvc.perform(post("/design/workorder/1/package/2/instruction/confirm/3"))
                .andExpect(status().isOk());
        verify(docService).confirmInstruction(1L, 2L, 3L);
    }

    @Test
    void drawingDownloadAndPreview_delegateIds() throws Exception {
        mockMvc.perform(get("/design/workorder/1/package/2/drawing/download"))
                .andExpect(status().isOk());
        verify(docService).downloadDrawing(eq(1L), eq(2L), any());

        mockMvc.perform(get("/design/workorder/1/package/2/instruction/preview-url"))
                .andExpect(status().isOk());
        verify(docService).getInstructionPreviewUrl(1L, 2L);
    }

    @Test
    void drawingVersions_delegateIds() throws Exception {
        mockMvc.perform(get("/design/workorder/1/package/2/drawing/versions"))
                .andExpect(status().isOk());
        verify(docService).listDrawingVersions(1L, 2L);
    }

    @Test
    void screenshotQuery_delegatesPackageAndFileIds() throws Exception {
        mockMvc.perform(get("/design/workorder/1/package/2/files/3/screenshot"))
                .andExpect(status().isOk());
        verify(screenshotService).getScreenshot(2L, 3L);
    }
}
