package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.basic.file.service.impl.FileRecorderService;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassicCaseFileServiceImplTest {

    @Mock
    private OrderFileMapper orderFileMapper;
    @Mock
    private FileDetailMapper fileDetailMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private FileRecorderService fileRecorderService;
    @Mock
    private MovePretreatment movePretreatment;

    @InjectMocks
    private ClassicCaseFileServiceImpl fileService;

    @Test
    void collectOrderFileIds_withFiles() {
        Long orderId = 1L;

        OrderFileEntity orderFile1 = new OrderFileEntity();
        orderFile1.setFileId("file1");
        OrderFileEntity orderFile2 = new OrderFileEntity();
        orderFile2.setFileId("file2");

        when(orderFileMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(orderFile1, orderFile2));

        List<String> fileIds = fileService.collectOrderFileIds(orderId);

        assertEquals(2, fileIds.size());
        assertTrue(fileIds.contains("file1"));
        assertTrue(fileIds.contains("file2"));
    }

    @Test
    void collectOrderFileIds_noFiles() {
        when(orderFileMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<String> fileIds = fileService.collectOrderFileIds(1L);

        assertTrue(fileIds.isEmpty());
    }

    @Test
    void migrateFilesToClassicCase_success() {
        Long orderId = 1L;
        String orderCode = "ORD001";

        OrderFileEntity orderFile = new OrderFileEntity();
        orderFile.setFileId("file1");
        when(orderFileMapper.selectList(any())).thenReturn(Arrays.asList(orderFile));

        FileInfo oldFile = new FileInfo();
        oldFile.setId("file1");
        oldFile.setPath("orders/2024/");
        oldFile.setUrl("https://oss.example.com/orders/2024/test.pdf");
        FileInfo movedFile = new FileInfo();
        movedFile.setId("classic-file1");
        movedFile.setUrl("https://oss.example.com/classic-cases/ORD001/test.pdf");
        when(fileRecorderService.getById("file1")).thenReturn(oldFile);
        when(fileStorageService.move(oldFile)).thenReturn(movePretreatment);
        when(movePretreatment.setPath("classic-cases/ORD001/")).thenReturn(movePretreatment);
        when(movePretreatment.move()).thenReturn(movedFile);
        when(orderFileMapper.selectList(any())).thenReturn(Arrays.asList(orderFile), Arrays.asList(orderFile));

        fileService.migrateFilesToClassicCase(orderId, orderCode);

        verify(fileStorageService).move(oldFile);
        ArgumentCaptor<OrderFileEntity> captor = ArgumentCaptor.forClass(OrderFileEntity.class);
        verify(orderFileMapper).updateById(captor.capture());
        assertEquals("classic-file1", captor.getValue().getFileId());
    }
}
