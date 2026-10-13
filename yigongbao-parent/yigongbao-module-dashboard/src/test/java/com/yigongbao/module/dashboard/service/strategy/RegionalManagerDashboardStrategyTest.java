package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.vo.DashboardVO;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserManagedOrgService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalManagerDashboardStrategyTest {

    @Mock
    private OrderMainMapper orderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserManagedOrgService userManagedOrgService;

    @InjectMocks
    private RegionalManagerDashboardStrategy strategy;

    @Test
    void pendingCardDoesNotUseRegionalAuditStatus() {
        when(userManagedOrgService.getEffectiveOrgIds(1L)).thenReturn(List.of(100L, 200L));
        when(userMapper.selectCount(any())).thenReturn(2L);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectMaps(any())).thenReturn(List.<Map<String, Object>>of());

        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setTimeRange("today");
        DashboardVO dashboard = strategy.buildDashboard(1L, query);

        assertThat(dashboard.getCards()).extracting("title").contains("待设计审核订单");
        ArgumentCaptor<QueryWrapper<OrderMainEntity>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(orderMapper, atLeast(4)).selectCount(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(wrapper -> !wrapper.getSqlSegment().contains("regional_audit_status"));
        assertThat(captor.getAllValues()).allMatch(wrapper -> wrapper.getSqlSegment().contains("org_id"));
        QueryWrapper<OrderMainEntity> pendingWrapper = captor.getAllValues().stream()
                .filter(wrapper -> wrapper.getParamNameValuePairs()
                        .containsValue(FlowStatusEnum.PENDING_DATA_AUDIT.getValue()))
                .findFirst().orElseThrow();
        assertThat(pendingWrapper.getSqlSegment()).contains("create_time", "BETWEEN");
    }
}
