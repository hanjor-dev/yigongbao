package com.yigongbao.module.production.warehouse.service.impl;

import com.yigongbao.module.production.warehouse.dto.WarehouseStatisticsQueryDTO;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.warehouse.vo.WarehouseStatisticsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseStatisticsServiceTest {

    @Mock private ProductionRecordMapper recordMapper;

    @InjectMocks
    private WarehouseServiceImpl service;

    @Test
    void getStatistics_returnsProductWarehouseCounts() {
        WarehouseStatisticsVO expected = new WarehouseStatisticsVO();
        expected.setTotal(12L);
        expected.setPendingWarehouseIn(3L);
        expected.setWarehoused(5L);
        expected.setWarehouseOut(4L);
        when(recordMapper.selectWarehouseStatistics(any())).thenReturn(expected);

        WarehouseStatisticsVO actual = service.getStatistics(new WarehouseStatisticsQueryDTO());

        assertEquals(12L, actual.getTotal());
        assertEquals(3L, actual.getPendingWarehouseIn());
        assertEquals(5L, actual.getWarehoused());
        assertEquals(4L, actual.getWarehouseOut());
    }
}
