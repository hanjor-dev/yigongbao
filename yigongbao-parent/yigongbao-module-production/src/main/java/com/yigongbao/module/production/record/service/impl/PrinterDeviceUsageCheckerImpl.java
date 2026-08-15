package com.yigongbao.module.production.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.service.PrinterDeviceUsageChecker;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrinterDeviceUsageCheckerImpl implements PrinterDeviceUsageChecker {

    private final ProductionRecordMapper recordMapper;

    @Override
    public Set<Long> findActiveDeviceIds(Collection<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> requestedDeviceIds = deviceIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (requestedDeviceIds.isEmpty()) {
            return Set.of();
        }

        List<ProductionRecordEntity> records = recordMapper.selectList(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .select(ProductionRecordEntity::getPrintDeviceId)
                        .in(ProductionRecordEntity::getPrintDeviceId, requestedDeviceIds)
                        .eq(ProductionRecordEntity::getIsDeleted, 0)
                        .in(ProductionRecordEntity::getStatus,
                                FlowStatusEnum.PENDING_PRINT.getValue(),
                                FlowStatusEnum.PRINTING.getValue()));
        if (records == null || records.isEmpty()) {
            return Set.of();
        }

        return records.stream()
                .filter(Objects::nonNull)
                .map(ProductionRecordEntity::getPrintDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isInUseByOtherRecord(Long deviceId, Long excludedRecordId) {
        if (deviceId == null) {
            return false;
        }

        LambdaQueryWrapper<ProductionRecordEntity> query = new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
                .eq(ProductionRecordEntity::getIsDeleted, 0)
                .in(ProductionRecordEntity::getStatus,
                        FlowStatusEnum.PENDING_PRINT.getValue(),
                        FlowStatusEnum.PRINTING.getValue())
                .ne(excludedRecordId != null, ProductionRecordEntity::getId, excludedRecordId);
        return recordMapper.selectCount(query) > 0;
    }
}
