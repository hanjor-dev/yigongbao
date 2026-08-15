package com.yigongbao.module.production.record.service.impl;

import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.ProductionTestConfiguration;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = ProductionTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class PrinterDeviceUsageCheckerImplTest {

    @Autowired
    private PrinterDeviceUsageChecker checker;

    @Autowired
    private ProductionRecordMapper recordMapper;

    @Test
    void findsOnlyRequestedNonDeletedDevicesWithActivePrintStatus() {
        insertRecord(10L, FlowStatusEnum.PENDING_PRINT, 0);
        insertRecord(20L, FlowStatusEnum.PRINTING, 0);
        insertRecord(30L, FlowStatusEnum.PRINT_COMPLETED, 0);
        insertRecord(30L, FlowStatusEnum.PRINTING, 1);
        insertRecord(40L, FlowStatusEnum.PRINTING, 0);

        assertThat(checker.findActiveDeviceIds(List.of(10L, 20L, 30L)))
                .isEqualTo(Set.of(10L, 20L));
        assertThat(checker.isInUse(10L)).isTrue();
        assertThat(checker.findActiveDeviceIds(List.of())).isEmpty();
    }

    @Test
    void ignoresNullDeviceIds() {
        insertRecord(10L, FlowStatusEnum.PENDING_PRINT, 0);

        assertThat(checker.findActiveDeviceIds(Arrays.asList(null, 10L, null)))
                .isEqualTo(Set.of(10L));
    }

    @Test
    void nullEmptyAndAllNullInputsReturnEmptyWithoutQueryingMapper() {
        ProductionRecordMapper mapper = mock(ProductionRecordMapper.class);
        PrinterDeviceUsageCheckerImpl directChecker = new PrinterDeviceUsageCheckerImpl(mapper);

        assertThat(directChecker.findActiveDeviceIds(null)).isNotNull().isEmpty();
        assertThat(directChecker.findActiveDeviceIds(List.of())).isNotNull().isEmpty();
        assertThat(directChecker.findActiveDeviceIds(Arrays.asList(null, null))).isNotNull().isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    void excludesCurrentRecordButStillFindsOtherActiveRecordsOnTheSameDevice() {
        insertRecord(1001L, 50L, FlowStatusEnum.PENDING_PRINT, 0);
        insertRecord(1002L, 50L, FlowStatusEnum.PRINTING, 0);
        insertRecord(1003L, 50L, FlowStatusEnum.PRINT_COMPLETED, 0);
        insertRecord(1004L, 50L, FlowStatusEnum.PRINTING, 1);

        assertThat(checker.isInUseByOtherRecord(50L, 1001L)).isTrue();
    }

    @Test
    void returnsFalseWhenTheCurrentRecordIsTheOnlyActiveRecordOnTheDevice() {
        insertRecord(2001L, 60L, FlowStatusEnum.PENDING_PRINT, 0);

        assertThat(checker.isInUseByOtherRecord(60L, 2001L)).isFalse();
    }

    @Test
    void nullDeviceReturnsFalseWithoutQueryingMapper() {
        ProductionRecordMapper mapper = mock(ProductionRecordMapper.class);
        PrinterDeviceUsageCheckerImpl directChecker = new PrinterDeviceUsageCheckerImpl(mapper);

        assertThat(directChecker.isInUseByOtherRecord(null, 2001L)).isFalse();
        verifyNoInteractions(mapper);
    }

    private void insertRecord(Long deviceId, FlowStatusEnum status, int isDeleted) {
        insertRecord(null, deviceId, status, isDeleted);
    }

    private void insertRecord(Long id, Long deviceId, FlowStatusEnum status, int isDeleted) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(id);
        record.setPrintDeviceId(deviceId);
        record.setStatus(status.getValue());
        record.setIsDeleted(isDeleted);
        assertThat(recordMapper.insert(record)).isEqualTo(1);
    }
}
