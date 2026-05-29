package com.yigongbao.module.production.pack.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

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
        // 校验流转卡状态：必须是包装中才能填写包装信息
        if (!FlowStatusEnum.PACKING.getValue().equals(record.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_NOT_IN_PACKING_STATUS);
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
        Long userId = StpUtil.getLoginIdAsLong();
        record.setPackOperatorId(userId);
        UserEntity user = userMapper.selectById(userId);
        record.setPackOperatorName(user != null ? user.getRealName() : null);
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
        // 幂等性保护：只有在包装状态下才能流转到入库
        if (!FlowStatusEnum.PACKING.getValue().equals(record.getStatus())) {
            log.warn("流转卡状态不允许流转到入库: recordId={}, currentStatus={}",
                recordId, record.getStatus());
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_WAREHOUSE_IN);
        }
        record.setStatus(FlowStatusEnum.WAREHOUSE_IN.getValue());
        recordMapper.updateById(record);
        recordService.triggerFlowIfAllExact(record.getOrderId(),
                FlowStatusEnum.WAREHOUSE_IN.getValue(), FlowActionEnum.COMPLETE_WAREHOUSE_IN);
        log.info("包装完成，流转到入库: recordId={}, recordNo={}, orderId={}", recordId, record.getRecordNo(), record.getOrderId());
    }
}
