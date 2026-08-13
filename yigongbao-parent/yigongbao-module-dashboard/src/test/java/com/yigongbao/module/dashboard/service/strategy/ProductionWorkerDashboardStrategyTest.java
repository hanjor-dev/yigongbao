package com.yigongbao.module.dashboard.service.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yigongbao.module.dashboard.dto.DashboardQueryDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
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
class ProductionWorkerDashboardStrategyTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(ProductionRecordEntity.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), ProductionRecordEntity.class);
        }
    }

    @Mock
    private ProductionRecordMapper recordMapper;

    @InjectMocks
    private ProductionWorkerDashboardStrategy strategy;

    @Test
    void currentBacklogCardsDoNotFilterByRecordCreateTime() {
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.selectMaps(any())).thenReturn(List.<Map<String, Object>>of());

        strategy.buildDashboard(25L, query("today"));

        ArgumentCaptor<LambdaQueryWrapper<ProductionRecordEntity>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(recordMapper, atLeast(5)).selectCount(captor.capture());
        List<LambdaQueryWrapper<ProductionRecordEntity>> wrappers = captor.getAllValues();

        assertThat(wrappers.get(0).getSqlSegment()).contains("createTime");
        assertThat(wrappers.subList(1, 4))
                .allMatch(wrapper -> !wrapper.getSqlSegment().contains("createTime"));
    }

    @Test
    void mapperFailureIsNotDisguisedAsEmptyDashboard() {
        when(recordMapper.selectCount(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> strategy.buildDashboard(25L, query("today")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");
    }

    @Test
    void periodQueriesUseLeftClosedRightOpenTimeConditions() {
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.selectMaps(any())).thenReturn(List.<Map<String, Object>>of());

        strategy.buildDashboard(25L, query("today"));

        ArgumentCaptor<QueryWrapper<ProductionRecordEntity>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(recordMapper, atLeast(5)).selectMaps(captor.capture());
        List<QueryWrapper<ProductionRecordEntity>> periodWrappers = captor.getAllValues().stream()
                .filter(wrapper -> wrapper.getSqlSegment().contains("create_time")
                        || wrapper.getSqlSegment().contains("post_processing_end_time"))
                .toList();
        assertThat(periodWrappers).hasSize(5).allMatch(wrapper -> {
                    String sql = wrapper.getSqlSegment();
                    String column = sql.contains("post_processing_end_time")
                            ? "post_processing_end_time" : "create_time";
                    return sql.contains(column + " >= ")
                            && sql.contains(column + " < ")
                            && !sql.contains(column + " BETWEEN");
                });
    }

    @Test
    void monthComparisonIncludesFifthWeekBucket() {
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.selectMaps(any())).thenReturn(List.<Map<String, Object>>of());

        var dashboard = strategy.buildDashboard(25L, query("month"));

        Object data = dashboard.getCharts().stream()
                .filter(chart -> "monthComparison".equals(chart.getKey()))
                .findFirst().orElseThrow().getData();
        assertThat(data).hasFieldOrPropertyWithValue("xAxis",
                List.of("第1周", "第2周", "第3周", "第4周", "第5周"));
    }

    private DashboardQueryDTO query(String range) {
        DashboardQueryDTO query = new DashboardQueryDTO();
        query.setTimeRange(range);
        return query;
    }
}
