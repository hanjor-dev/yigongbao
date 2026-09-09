package com.yigongbao.module.production.record.service.impl;

import com.yigongbao.module.production.record.dto.ProductionRecordStatisticsQueryDTO;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.vo.ProductionRecordStatisticsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionRecordStatisticsServiceTest {

    @Mock private ProductionRecordMapper recordMapper;

    @InjectMocks
    private ProductionRecordServiceImpl service;

    @Test
    void getStatistics_returnsStatusCounts() {
        ProductionRecordStatisticsQueryDTO query = new ProductionRecordStatisticsQueryDTO();
        query.setProcessingCenterId(9L);
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
        ProductionRecordStatisticsVO expected = new ProductionRecordStatisticsVO();
        expected.setTotal(15L);
        expected.setPendingPrint(2L);
        expected.setQcInProgress(4L);
        expected.setQcCompleted(6L);
        when(recordMapper.selectStatistics(any(), any(), any())).thenReturn(expected);

        ProductionRecordStatisticsVO actual = service.getStatistics(query);

        assertEquals(15L, actual.getTotal());
        assertEquals(2L, actual.getPendingPrint());
        assertEquals(4L, actual.getQcInProgress());
        assertEquals(6L, actual.getQcCompleted());
    }
}
