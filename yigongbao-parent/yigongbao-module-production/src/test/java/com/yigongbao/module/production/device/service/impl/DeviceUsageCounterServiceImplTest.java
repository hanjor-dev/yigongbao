package com.yigongbao.module.production.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.device.entity.DeviceUsageCounterEntity;
import com.yigongbao.module.production.device.mapper.DeviceUsageCounterMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceUsageCounterServiceImplTest {

    @BeforeAll
    static void initLambdaCache() {
        Configuration configuration = new Configuration();
        GlobalConfigUtils.getGlobalConfig(configuration);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), DeviceUsageCounterEntity.class);
    }

    @Mock private DeviceUsageCounterMapper counterMapper;

    @Spy
    @InjectMocks
    private DeviceUsageCounterServiceImpl service;

    @Test
    void incrementAndGet_insertsFirstDailyCounterAtOne() {
        when(counterMapper.selectOne(any())).thenReturn(null);

        assertThat(service.incrementAndGet(7L)).isEqualTo(1);

        var captor = org.mockito.ArgumentCaptor.forClass(DeviceUsageCounterEntity.class);
        verify(counterMapper).insert(captor.capture());
        DeviceUsageCounterEntity inserted = captor.getValue();
        assertThat(inserted.getDeviceId()).isEqualTo(7L);
        assertThat(inserted.getUsageCount()).isEqualTo(1);
        assertThat(inserted.getVersion()).isEqualTo(0);
    }

    @Test
    void incrementAndGetAtDate_usesProvidedDateForDailyCounter() {
        when(counterMapper.selectOne(any())).thenReturn(null);
        LocalDate assignmentDate = LocalDate.of(2026, 9, 1);

        assertThat(service.incrementAndGet(7L, assignmentDate)).isEqualTo(1);

        var captor = org.mockito.ArgumentCaptor.forClass(DeviceUsageCounterEntity.class);
        verify(counterMapper).insert(captor.capture());
        assertThat(captor.getValue().getUsageDate()).isEqualTo(assignmentDate);
    }

    @Test
    void incrementAndGet_updatesExistingCounterWithOptimisticVersion() {
        DeviceUsageCounterEntity counter = new DeviceUsageCounterEntity();
        counter.setId(10L);
        counter.setDeviceId(7L);
        counter.setUsageCount(3);
        counter.setVersion(2);
        when(counterMapper.selectOne(any())).thenReturn(counter);
        when(counterMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertThat(service.incrementAndGet(7L)).isEqualTo(4);

        verify(counterMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void incrementAndGet_throwsAfterRepeatedOptimisticConflicts() {
        DeviceUsageCounterEntity counter = new DeviceUsageCounterEntity();
        counter.setId(10L);
        counter.setDeviceId(7L);
        counter.setUsageCount(3);
        counter.setVersion(2);
        when(counterMapper.selectOne(any())).thenReturn(counter);
        when(counterMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.incrementAndGet(7L));

        verify(counterMapper, times(3)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void getTodayCount_returnsStoredCount() {
        DeviceUsageCounterEntity counter = new DeviceUsageCounterEntity();
        counter.setDeviceId(7L);
        counter.setUsageCount(6);
        when(counterMapper.selectOne(any())).thenReturn(counter);

        assertThat(service.getTodayCount(7L)).isEqualTo(6);
    }

    @Test
    void getTodayCount_returnsZeroWhenNoDailyCounterExists() {
        when(counterMapper.selectOne(any())).thenReturn(null);

        assertThat(service.getTodayCount(7L)).isZero();
    }
}
