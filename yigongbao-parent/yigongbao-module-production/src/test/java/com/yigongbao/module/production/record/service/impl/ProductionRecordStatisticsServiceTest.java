package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.module.production.record.dto.ProductionRecordStatisticsQueryDTO;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.vo.ProductionRecordStatisticsVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionRecordStatisticsServiceTest {

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserHospitalService userHospitalService;

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

    @Test
    void getStatistics_allowsDesignerToQueryAllRecords() {
        ProductionRecordStatisticsQueryDTO query = new ProductionRecordStatisticsQueryDTO();
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
        UserEntity designer = new UserEntity();
        designer.setRoleCode(RoleCodeEnum.DESIGNER.getCode());
        designer.setCenterId(9L);
        when(userMapper.selectById(1L)).thenReturn(designer);
        when(userHospitalService.getDataScopeType(1L)).thenReturn(DataScopeTypeEnum.CENTER);
        when(recordMapper.selectStatistics(any(), any(), any())).thenReturn(new ProductionRecordStatisticsVO());

        try (var stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            service.getStatistics(query);

            verify(recordMapper).selectStatistics(eq(query), eq("ALL"), eq(null));
        }
    }
}
