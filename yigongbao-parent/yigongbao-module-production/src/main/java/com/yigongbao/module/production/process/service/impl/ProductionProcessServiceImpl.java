package com.yigongbao.module.production.process.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.process.vo.ProcessVO;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工序操作服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionProcessServiceImpl extends ServiceImpl<ProductionProcessMapper, ProductionProcessEntity>
        implements IProductionProcessService {

    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final IProductionRecordService recordService;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;

    @Override
    public List<ProcessVO> listProcesses(Long recordId) {
        List<ProductionProcessEntity> processes = list(
                new LambdaQueryWrapper<ProductionProcessEntity>()
                        .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                        .orderByAsc(ProductionProcessEntity::getProcessOrder));
        return processes.stream().map(p -> {
            ProcessVO vo = new ProcessVO();
            BeanUtil.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcess(Long recordId, StartProcessDTO dto) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        ProductionProcessEntity process = getOne(new LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, dto.getProcessType()));
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND);
        }
        if (!ProcessStatusEnum.PENDING.getCode().equals(process.getStatus())) {
            throw new BusinessException(400, "工序已开始或已完成，无法重复开始");
        }
        process.setDeviceId(dto.getPrimaryDeviceId());
        DeviceEntity device = deviceMapper.selectById(dto.getPrimaryDeviceId());
        if (device != null) {
            String expectedDeviceType = getExpectedDeviceType(dto.getProcessType());
            if (expectedDeviceType != null && !expectedDeviceType.equals(device.getDeviceType())) {
                throw new BusinessException(400, "设备类型与工序不匹配，请选择正确的设备");
            }
            process.setDeviceNo(device.getDeviceId());
            process.setDeviceName(device.getDeviceName());
        }
        process.setProcessParams(dto.getProcessParams());
        process.setStartTime(LocalDateTime.now());
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity operator = userMapper.selectById(userId);
        process.setOperatorId(userId);
        process.setOperatorName(operator != null ? operator.getRealName() : null);
        process.setStatus(ProcessStatusEnum.IN_PROGRESS.getCode());
        updateById(process);
        if (!ProcessTypeEnum.PRINT.getCode().equals(dto.getProcessType())) {
            record.setStatus(FlowStatusEnum.POST_PROCESSING.getValue());
            record.setCurrentProcess(dto.getProcessType());
            recordMapper.updateById(record);
        }
        log.info("开始工序: recordId={}, processType={}, deviceId={}", recordId, dto.getProcessType(), dto.getPrimaryDeviceId());
    }

    /**
     * 完成工序：标记工序为已完成，后处理工序自动推进到下一工序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishProcess(Long recordId, String processType) {
        ProductionProcessEntity process = getOne(new LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, processType));
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND);
        }
        if (!ProcessStatusEnum.IN_PROGRESS.getCode().equals(process.getStatus())) {
            throw new BusinessException(400, "工序未在进行中，无法完成");
        }
        LocalDateTime endTime;
        if (process.getDeviceId() != null) {
            DeviceEntity device = deviceMapper.selectById(process.getDeviceId());
            if (device != null && device.getProcessingMinutes() != null && device.getProcessingMinutes() > 0) {
                endTime = process.getStartTime().plusMinutes(device.getProcessingMinutes());
            } else {
                endTime = LocalDateTime.now();
            }
        } else {
            endTime = LocalDateTime.now();
        }
        process.setEndTime(endTime);
        process.setStatus(ProcessStatusEnum.COMPLETED.getCode());
        updateById(process);

        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) return;

        if (ProcessTypeEnum.WASH.getCode().equals(processType)) {
            record.setCurrentProcess(ProcessTypeEnum.CURE.getCode());
            recordMapper.updateById(record);
        } else if (ProcessTypeEnum.CURE.getCode().equals(processType)) {
            record.setCurrentProcess(ProcessTypeEnum.CLEAN_DRY.getCode());
            recordMapper.updateById(record);
        } else if (ProcessTypeEnum.CLEAN_DRY.getCode().equals(processType)) {
            record.setStatus(FlowStatusEnum.QC_IN_PROGRESS.getValue());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    FlowStatusEnum.QC_IN_PROGRESS.getValue(), FlowActionEnum.COMPLETE_POST_PROCESSING);
        }
        log.info("完成工序: recordId={}, processType={}", recordId, processType);
    }

    private String getExpectedDeviceType(String processType) {
        return switch (processType) {
            case "wash" -> DeviceTypeEnum.WASH_CONTAINER.getCode();
            case "cure" -> DeviceTypeEnum.UV_CURING.getCode();
            case "clean_dry" -> DeviceTypeEnum.ULTRASONIC_CLEANER.getCode();
            case "pack" -> DeviceTypeEnum.SEALING_MACHINE.getCode();
            default -> null;
        };
    }
}
