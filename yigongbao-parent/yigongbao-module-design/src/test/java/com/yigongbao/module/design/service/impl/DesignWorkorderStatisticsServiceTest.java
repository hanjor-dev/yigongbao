package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.design.dto.DesignWorkorderStatisticsQueryDTO;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.mapper.DesignWorkorderStatisticsMapper;
import com.yigongbao.module.design.vo.DesignWorkorderStatisticsVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.order.service.OrderMainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DesignWorkorderStatisticsServiceTest {

    @Mock private OrderMainService orderMainService;
    @Mock private DesignQueryHelper designQueryHelper;
    @Mock private UserHospitalService userHospitalService;
    @Mock private DesignWorkorderStatisticsMapper statisticsMapper;

    @InjectMocks
    private DesignWorkorderServiceImpl service;

    @Test
    void getStatistics_returnsAggregatedCountsWithDesignScope() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        when(designQueryHelper.getCurrentUserId()).thenReturn(7L);
        when(designQueryHelper.getCurrentUser()).thenReturn(user);
        when(userHospitalService.getDataScopeType(7L)).thenReturn(DataScopeTypeEnum.ALL);

        DesignWorkorderStatisticsVO expected = new DesignWorkorderStatisticsVO();
        expected.setTotal(10L);
        expected.setPendingDesign(2L);
        expected.setDesigning(3L);
        expected.setDesignCompleted(1L);
        when(statisticsMapper.selectStatistics(any(Wrapper.class))).thenReturn(expected);

        DesignWorkorderStatisticsVO actual = service.getStatistics(new DesignWorkorderStatisticsQueryDTO());

        assertEquals(10L, actual.getTotal());
        assertEquals(2L, actual.getPendingDesign());
        assertEquals(3L, actual.getDesigning());
        assertEquals(1L, actual.getDesignCompleted());
    }
}
