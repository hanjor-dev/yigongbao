package com.yigongbao.module.basic.processingCenter.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.impl.ProcessingCenterServiceImpl;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessingCenterServiceImplTest {

    @Mock
    private ProcessingCenterMapper processingCenterMapper;

    @InjectMocks
    private ProcessingCenterServiceImpl processingCenterService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(processingCenterService, processingCenterMapper);
    }

    @Test
    void testCreateProcessingCenter_Success() {
        CreateProcessingCenterDTO dto = new CreateProcessingCenterDTO();
        dto.setCenterCode("WH001");
        dto.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectCount(any())).thenReturn(0L);
        when(processingCenterMapper.insert(any(ProcessingCenterEntity.class))).thenAnswer(invocation -> {
            ProcessingCenterEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });

        Long id = processingCenterService.createProcessingCenter(dto);

        assertNotNull(id);
        assertEquals(1L, id);
        verify(processingCenterMapper, times(1)).insert(any(ProcessingCenterEntity.class));
    }

    @Test
    void testCreateProcessingCenter_DuplicateCode() {
        CreateProcessingCenterDTO dto = new CreateProcessingCenterDTO();
        dto.setCenterCode("WH001");
        dto.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> {
            processingCenterService.createProcessingCenter(dto);
        });
    }

    @Test
    void testGetProcessingCenterById_Success() {
        ProcessingCenterEntity entity = new ProcessingCenterEntity();
        entity.setId(1L);
        entity.setCenterCode("WH001");
        entity.setCenterName("武汉嘉一");

        when(processingCenterMapper.selectById(1L)).thenReturn(entity);

        ProcessingCenterVO vo = processingCenterService.getProcessingCenterById(1L);

        assertNotNull(vo);
        assertEquals("WH001", vo.getCenterCode());
        assertEquals("武汉嘉一", vo.getCenterName());
    }

    @Test
    void testGetProcessingCenterById_NotFound() {
        when(processingCenterMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            processingCenterService.getProcessingCenterById(999L);
        });
    }
}
