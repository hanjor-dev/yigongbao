package com.yigongbao.module.production.pack.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import com.yigongbao.module.production.pack.service.IProductionPackService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 包装服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionPackServiceImpl implements IProductionPackService {

    private final ProductionRecordMapper recordMapper;
    private final DeviceMapper deviceMapper;
    private final IProductionRecordService recordService;

    /**
     * 填写包装信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillPackInfo(Long recordId, FillPackDTO dto) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        DeviceEntity device = deviceMapper.selectById(dto.getPackDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.PACK_DEVICE_NOT_FOUND);
        }
        record.setPackDeviceId(dto.getPackDeviceId());
        record.setPackDeviceNo(device.getDeviceId());
        record.setPackSealTemperature(dto.getPackSealTemperature());
        record.setPackSealTime(dto.getPackSealTime());
        record.setPackSterilizationMethod(dto.getPackSterilizationMethod());
        record.setPackSterilizationBatchNo(dto.getPackSterilizationBatchNo());
        record.setPackOperatorId(StpUtil.getLoginIdAsLong());
        record.setPackTime(LocalDateTime.now());
        recordMapper.updateById(record);
        log.info("填写包装信息: recordId={}, recordNo={}, packDeviceId={}", recordId, record.getRecordNo(), dto.getPackDeviceId());
    }

    /**
     * 包装完成，流转到入库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToWarehouse(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (record.getPackDeviceId() == null) {
            throw new BusinessException(ErrorCodeEnum.PACK_INFO_NOT_FILLED);
        }
        record.setStatus(RecordStatusEnum.WAREHOUSE_IN.getCode());
        recordMapper.updateById(record);
        recordService.triggerFlowIfAllReach(record.getOrderId(),
                RecordStatusEnum.WAREHOUSE_IN.getCode(), FlowActionEnum.COMPLETE_WAREHOUSE_IN);
        log.info("包装完成，流转到入库: recordId={}, recordNo={}, orderId={}", recordId, record.getRecordNo(), record.getOrderId());
    }
}
