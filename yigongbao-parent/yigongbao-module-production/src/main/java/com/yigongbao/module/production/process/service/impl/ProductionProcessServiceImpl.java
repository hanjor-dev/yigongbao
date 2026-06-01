package com.yigongbao.module.production.process.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

    /** 查询流转卡的工序列表，按工序顺序升序排列 */
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

    /** 开始工序：记录设备、操作员、开始时间；非打印工序同步更新流转卡状态为后处理中 */
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
            throw new BusinessException(ErrorCodeEnum.PROCESS_ALREADY_STARTED);
        }
        // 校验流转卡状态：拒绝已取消或打印失败的流转卡
        if (FlowStatusEnum.CANCELLED.getValue().equals(record.getStatus()) ||
            FlowStatusEnum.PRINT_FAILED.getValue().equals(record.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL);
        }
        // 根据工序类型校验流转卡状态
        if (ProcessTypeEnum.PRINT.getCode().equals(dto.getProcessType())) {
            if (!FlowStatusEnum.PENDING_PRINT.getValue().equals(record.getStatus()) &&
                !FlowStatusEnum.PRINTING.getValue().equals(record.getStatus())) {
                throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_START_PRINT);
            }
        } else {
            // 后处理工序（洗、固化、清洗干燥）需要在打印完成或后处理中状态
            if (!FlowStatusEnum.PRINT_COMPLETED.getValue().equals(record.getStatus()) &&
                !FlowStatusEnum.POST_PROCESSING.getValue().equals(record.getStatus())) {
                throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_START_POST_PROCESS);
            }
        }
        process.setDeviceId(dto.getPrimaryDeviceId());
        DeviceEntity device = deviceMapper.selectById(dto.getPrimaryDeviceId());
        if (device != null) {
            String expectedDeviceType = getExpectedDeviceType(dto.getProcessType());
            if (expectedDeviceType != null && !expectedDeviceType.equals(device.getDeviceType())) {
                throw new BusinessException(ErrorCodeEnum.DEVICE_TYPE_MISMATCH);
            }
            process.setDeviceNo(device.getDeviceId());
            process.setDeviceName(device.getDeviceName());
        }
        if (dto.getSecondaryDeviceId() != null) {
            process.setSecondaryDeviceId(dto.getSecondaryDeviceId());
            DeviceEntity secondaryDevice = deviceMapper.selectById(dto.getSecondaryDeviceId());
            if (secondaryDevice != null) {
                process.setSecondaryDeviceNo(secondaryDevice.getDeviceId());
                process.setSecondaryDeviceName(secondaryDevice.getDeviceName());
            }
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
            record.setContentUpdateTime(LocalDateTime.now());
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
            throw new BusinessException(ErrorCodeEnum.PROCESS_NOT_IN_PROGRESS);
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
            record.setContentUpdateTime(LocalDateTime.now());
            recordMapper.updateById(record);
        } else if (ProcessTypeEnum.CURE.getCode().equals(processType)) {
            record.setCurrentProcess(ProcessTypeEnum.CLEAN_DRY.getCode());
            record.setContentUpdateTime(LocalDateTime.now());
            recordMapper.updateById(record);
        } else if (ProcessTypeEnum.CLEAN_DRY.getCode().equals(processType)) {
            // 校验产品数量：确保流转卡有产品才能进入质检
            long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<ProductionProductEntity>()
                    .eq(ProductionProductEntity::getProductionRecordId, recordId)
                    .ne(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode()));
            if (productCount == 0) {
                throw new BusinessException(ErrorCodeEnum.RECORD_NO_PRODUCT_FOR_QC);
            }
            recordMapper.update(null,
                    new LambdaUpdateWrapper<ProductionRecordEntity>()
                            .eq(ProductionRecordEntity::getId, recordId)
                            .set(ProductionRecordEntity::getStatus, FlowStatusEnum.QC_IN_PROGRESS.getValue())
                            .set(ProductionRecordEntity::getCurrentProcess, null)
                            .set(ProductionRecordEntity::getContentUpdateTime, LocalDateTime.now()));
            recordService.triggerFlowIfAllExact(record.getOrderId(),
                    FlowStatusEnum.QC_IN_PROGRESS.getValue(), FlowActionEnum.COMPLETE_POST_PROCESSING);
        }
        log.info("完成工序: recordId={}, processType={}", recordId, processType);
    }

    /** 根据工序类型返回期望的设备类型码，用于校验分配设备是否匹配 */
    private String getExpectedDeviceType(String processType) {
        return switch (processType) {
            case "print" -> DeviceTypeEnum.PRINTER_SLA.getCode();
            case "wash" -> DeviceTypeEnum.WASH_CONTAINER.getCode();
            case "cure" -> DeviceTypeEnum.UV_CURING.getCode();
            case "clean_dry" -> DeviceTypeEnum.ULTRASONIC_CLEANER.getCode();
            case "pack" -> DeviceTypeEnum.SEALING_MACHINE.getCode();
            default -> null;
        };
    }
}
