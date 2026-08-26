package com.yigongbao.module.basic.file.service.impl;

import com.yigongbao.module.basic.file.config.FileStorageProperties;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDownloadUrlServiceImplTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileStorageProperties fileStorageProperties;

    @Mock
    private GeneratePresignedUrlPretreatment pretreatment;

    @Mock
    private GeneratePresignedUrlResult result;

    @InjectMocks
    private FileDownloadUrlServiceImpl service;

    @Test
    void generate_shouldUseResponseHeaderOverrideAndReturnGeneratedUrl() {
        FileInfo fileInfo = fileInfo("中文图纸.xlsx");
        when(fileStorageProperties.getDownloadUrlExpireMinutes()).thenReturn(10L);
        when(fileStorageService.generatePresignedUrl()).thenReturn(pretreatment);
        when(pretreatment.setPlatform(anyString())).thenReturn(pretreatment);
        when(pretreatment.setPath(anyString())).thenReturn(pretreatment);
        when(pretreatment.setFilename(anyString())).thenReturn(pretreatment);
        when(pretreatment.setMethod(anyString())).thenReturn(pretreatment);
        when(pretreatment.setExpiration(org.mockito.ArgumentMatchers.any())).thenReturn(pretreatment);
        when(pretreatment.putResponseHeaders(anyString(), anyString())).thenReturn(pretreatment);
        when(pretreatment.generatePresignedUrl()).thenReturn(result);
        when(result.getUrl()).thenReturn("https://oss.example/download");

        String url = service.generate(fileInfo, fileInfo.getOriginalFilename());

        assertEquals("https://oss.example/download", url);
        verify(pretreatment).putResponseHeaders(
                "Content-Disposition",
                "attachment; filename=\"download.xlsx\"; filename*=UTF-8''%E4%B8%AD%E6%96%87%E5%9B%BE%E7%BA%B8.xlsx");
    }

    @Test
    void generateBatch_shouldKeepOrderAndReturnNullForInvalidFile() {
        when(fileStorageProperties.getDownloadUrlExpireMinutes()).thenReturn(10L);
        FileInfo fileInfo = fileInfo("a.zip");
        when(fileStorageService.generatePresignedUrl()).thenReturn(pretreatment);
        when(pretreatment.setPlatform(anyString())).thenReturn(pretreatment);
        when(pretreatment.setPath(anyString())).thenReturn(pretreatment);
        when(pretreatment.setFilename(anyString())).thenReturn(pretreatment);
        when(pretreatment.setMethod(anyString())).thenReturn(pretreatment);
        when(pretreatment.setExpiration(org.mockito.ArgumentMatchers.any())).thenReturn(pretreatment);
        when(pretreatment.putResponseHeaders(anyString(), anyString())).thenReturn(pretreatment);
        when(pretreatment.generatePresignedUrl()).thenReturn(result);
        when(result.getUrl()).thenReturn("https://oss.example/a.zip");

        List<String> urls = service.generateBatch(List.of(
                new com.yigongbao.module.basic.file.service.FileDownloadUrlRequest(fileInfo, "a.zip"),
                new com.yigongbao.module.basic.file.service.FileDownloadUrlRequest(null, "b.zip")));

        assertEquals(Arrays.asList("https://oss.example/a.zip", null), urls);
        assertNull(service.generate(null, "a.zip"));
    }

    private FileInfo fileInfo(String originalFilename) {
        return new FileInfo()
                .setId("file-1")
                .setPlatform("aliyun-oss-1")
                .setPath("instruction_file/202608/")
                .setFilename("random.xlsx")
                .setOriginalFilename(originalFilename);
    }
}
