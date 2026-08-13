package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.dashboard.vo.BarChartDataVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionManagerDashboardStrategyTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(ProductionRecordEntity.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), ProductionRecordEntity.class);
        }
    }

    @Mock private ProductionRecordMapper recordMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProcessingCenterMapper centerMapper;
    @InjectMocks private ProductionManagerDashboardStrategy strategy;

    @Test
    void managerWithoutCenterGetsExplicitBusinessError() {
        UserEntity manager = manager(null);
        when(userMapper.selectById(25L)).thenReturn(manager);

        assertThatThrownBy(() -> strategy.buildDashboard(25L, query("today")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCodeEnum.PROCESSING_CENTER_NOT_FOUND.getCode()));
    }

    @Test
    void missingOrDisabledCenterGetsExplicitBusinessError() {
        when(userMapper.selectById(25L)).thenReturn(manager(1L));
        when(centerMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> strategy.buildDashboard(25L, query("today")))
                .isInstanceOf(BusinessException.class);

        ProcessingCenterEntity disabled = center(1L, 0, 0);
        when(centerMapper.selectById(1L)).thenReturn(disabled);
        assertThatThrownBy(() -> strategy.buildDashboard(25L, query("today")))
                .isInstanceOf(BusinessException.class);

        ProcessingCenterEntity deleted = center(1L, 1, 1);
        when(centerMapper.selectById(1L)).thenReturn(deleted);
        assertThatThrownBy(() -> strategy.buildDashboard(25L, query("today")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void currentBacklogCardsUseCenterButNotRecordCreateTime() {
        when(userMapper.selectById(25L)).thenReturn(manager(1L));
        when(centerMapper.selectById(1L)).thenReturn(center(1L, 1, 0));
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.selectMaps(any())).thenReturn(List.<Map<String, Object>>of());

        strategy.buildDashboard(25L, query("today"));

        ArgumentCaptor<LambdaQueryWrapper<ProductionRecordEntity>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recordMapper, atLeast(5)).selectCount(captor.capture());
        List<LambdaQueryWrapper<ProductionRecordEntity>> wrappers = captor.getAllValues();
        assertThat(wrappers).allMatch(w -> w.getSqlSegment().contains("processingCenterId"));
        assertThat(wrappers.get(0).getSqlSegment()).contains("createTime");
        assertThat(wrappers.subList(1, 4))
                .allMatch(wrapper -> !wrapper.getSqlSegment().contains("createTime"));
    }

    @Test
    void workloadSeparatesRealtimeBacklogFromPeriodCompleted() {
        when(userMapper.selectById(25L)).thenReturn(manager(1L));
        when(centerMapper.selectById(1L)).thenReturn(center(1L, 1, 0));
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.selectMaps(any())).thenReturn(List.<Map<String, Object>>of());

        strategy.buildDashboard(25L, query("today"));

        ArgumentCaptor<QueryWrapper<ProductionRecordEntity>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(recordMapper, atLeast(5)).selectMaps(captor.capture());
        assertThat(captor.getAllValues()).hasSize(5)
                .allMatch(wrapper -> wrapper.getSqlSegment().contains("processing_center_id"));
        assertThat(captor.getAllValues()).anyMatch(wrapper -> {
            String sql = wrapper.getSqlSegment();
            return sql.contains("producer_id IS NOT NULL")
                    && sql.contains("status BETWEEN")
                    && !sql.contains("create_time");
        });
        assertThat(captor.getAllValues()).anyMatch(wrapper -> {
            String sql = wrapper.getSqlSegment();
            return sql.contains("producer_id IS NOT NULL")
                    && sql.contains("post_processing_end_time >=")
                    && sql.contains("post_processing_end_time <")
                    && !sql.contains("create_time");
        });
    }

    @Test
    void workloadSumsRowsWhenOneProducerHasHistoricalNames() {
        when(userMapper.selectById(25L)).thenReturn(manager(1L));
        when(centerMapper.selectById(1L)).thenReturn(center(1L, 1, 0));
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.selectMaps(any())).thenAnswer(invocation -> {
            QueryWrapper<ProductionRecordEntity> wrapper = invocation.getArgument(0);
            if (wrapper.getSqlSelect() == null || !wrapper.getSqlSelect().contains("producer_id")) {
                return List.of();
            }
            if (wrapper.getSqlSegment().contains("post_processing_end_time")) {
                return List.of(
                        Map.of("producer_id", 26L, "producer_name", "旧名称", "count", 4),
                        Map.of("producer_id", 26L, "producer_name", "新名称", "count", 5));
            }
            return List.of(
                    Map.of("producer_id", 26L, "producer_name", "旧名称", "count", 2),
                    Map.of("producer_id", 26L, "producer_name", "新名称", "count", 3));
        });

        var dashboard = strategy.buildDashboard(25L, query("today"));

        BarChartDataVO workload = (BarChartDataVO) dashboard.getCharts().stream()
                .filter(chart -> "workerWorkload".equals(chart.getKey()))
                .findFirst().orElseThrow().getData();
        assertThat(workload.getXAxis()).hasSize(1);
        assertThat(workload.getSeries().get(0).getData()).containsExactly(5);
        assertThat(workload.getSeries().get(1).getData()).containsExactly(9);
    }

    private UserEntity manager(Long centerId) {
        UserEntity user = new UserEntity();
        user.setId(25L);
        user.setCenterId(centerId);
        return user;
    }

    private ProcessingCenterEntity center(Long id, int status, int deleted) {
        ProcessingCenterEntity center = new ProcessingCenterEntity();
        center.setId(id);
        center.setStatus(status);
        center.setIsDeleted(deleted);
        center.setCenterName("武汉嘉一");
        return center;
    }

    private DashboardQueryDTO query(String range) {
        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setTimeRange(range);
        return query;
    }
}
