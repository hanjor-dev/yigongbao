package com.yigongbao.module.design.service.impl;

import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.vo.DesignQrImageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignQrImageServiceImplTest {

    private static final Long ORDER_ID = 1L;
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };

    @Mock private FileService fileService;
    @Mock private DesignQueryHelper designQueryHelper;
    @InjectMocks private DesignQrImageServiceImpl service;

    private OrderMainEntity order;

    @BeforeEach
    void setUp() {
        order = new OrderMainEntity();
        order.setId(ORDER_ID);
        doNothing().when(designQueryHelper).checkIsAssignedDesigner(any());
        when(designQueryHelper.checkDesignPhase(ORDER_ID)).thenReturn(order);
    }

    @Test
    void uploadPng_replacesCurrentAssociation() {
        MultipartFile file = pngFile();
        FileVO uploaded = file("new-file");
        when(fileService.listByBiz("10.21", ORDER_ID)).thenReturn(Collections.emptyList());
        when(fileService.uploadFile(file, "10.21")).thenReturn(uploaded);
        when(fileService.linkFile("new-file", "10.21", ORDER_ID)).thenReturn(uploaded);

        DesignQrImageVO result = service.upload(ORDER_ID, file);

        assertEquals("new-file", result.getFileId());
        verify(fileService).unlinkByBiz("10.21", ORDER_ID);
        verify(fileService).linkFile("new-file", "10.21", ORDER_ID);
    }

    @Test
    void uploadSamePng_returnsCurrentFileWithoutUploadingAgain() {
        MultipartFile file = pngFile();
        FileVO current = file("current-file");
        current.setFileHash(md5(PNG));
        when(fileService.listByBiz("10.21", ORDER_ID)).thenReturn(java.util.List.of(current));

        DesignQrImageVO result = service.upload(ORDER_ID, file);

        assertEquals("current-file", result.getFileId());
        verify(fileService, never()).uploadFile(any(), eq("10.21"));
        verify(fileService, never()).unlinkByBiz(any(), any());
    }

    @Test
    void uploadInvalidPng_rejectsBeforeStorage() {
        MultipartFile file = new MockMultipartFile("file", "qr.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(BusinessException.class, () -> service.upload(ORDER_ID, file));

        verify(fileService, never()).uploadFile(any(), any());
    }

    @Test
    void uploadLinkFailure_deletesNewUnlinkedFile() {
        MultipartFile file = pngFile();
        FileVO uploaded = file("new-file");
        FileVO current = file("current-file");
        when(fileService.listByBiz("10.21", ORDER_ID)).thenReturn(Collections.emptyList());
        when(fileService.uploadFile(file, "10.21")).thenReturn(uploaded);
        when(fileService.linkFile("new-file", "10.21", ORDER_ID))
                .thenThrow(new IllegalStateException("link failed"));

        assertThrows(IllegalStateException.class, () -> service.upload(ORDER_ID, file));

        verify(fileService).deleteById("new-file");
    }

    @Test
    void uploadReplaceFailure_restoresPreviousAssociation() {
        MultipartFile file = pngFile();
        FileVO uploaded = file("new-file");
        FileVO current = file("current-file");
        when(fileService.listByBiz("10.21", ORDER_ID)).thenReturn(java.util.List.of(current));
        when(fileService.uploadFile(file, "10.21")).thenReturn(uploaded);
        when(fileService.linkFile("new-file", "10.21", ORDER_ID))
                .thenThrow(new IllegalStateException("link failed"));

        assertThrows(IllegalStateException.class, () -> service.upload(ORDER_ID, file));

        verify(fileService).linkFile("current-file", "10.21", ORDER_ID);
        verify(fileService).deleteById("new-file");
    }

    @Test
    void getCurrent_checksReadableOrderAndReturnsNullWhenMissing() {
        when(fileService.listByBiz("10.21", ORDER_ID)).thenReturn(Collections.emptyList());

        assertEquals(null, service.getCurrent(ORDER_ID));

        verify(designQueryHelper).checkOrderReadable(ORDER_ID);
    }

    private MultipartFile pngFile() {
        return new MockMultipartFile("file", "qr.png", "image/png", PNG);
    }

    private FileVO file(String id) {
        FileVO file = new FileVO();
        file.setId(id);
        file.setFileName("qr.png");
        file.setFileUrl("/api/files/public/" + id);
        file.setFileSize((long) PNG.length);
        return file;
    }

    private String md5(byte[] bytes) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("MD5").digest(bytes);
            StringBuilder result = new StringBuilder();
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
