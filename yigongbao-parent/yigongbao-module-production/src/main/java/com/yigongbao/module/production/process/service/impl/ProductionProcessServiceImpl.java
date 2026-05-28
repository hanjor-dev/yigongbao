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
import com.yigongbao.module.production.enums.QcResultEnum;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.dto.ProcessProductResultDTO;
import com.yigongbao.module.production.process.dto.StartProcessDTO;
import com.yigongbao.module.production.process.dto.SubmitProcessQcDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.process.vo.ProcessVO;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.qc.entity.ProductionProcessProductResultEntity;
import com.yigongbao.module.production.qc.mapper.ProductionProcessProductResultMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final ProductionProcessProductResultMapper processProductResultMapper;

    /**
     * 填写工序补充信息（设备、参数等），不触发推进
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillProcess(Long processId, FillProcessDTO dto) {
        ProductionProcessEntity process = getById(processId);
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND);
        }
        process.setDeviceId(dto.getDeviceId());
        process.setProcessParams(dto.getProcessParams());
        process.setHasRedo(dto.getHasRedo());
        process.setRedoRemark(dto.getRedoRemark());
        updateById(process);
        log.info("填写工序信息: processId={}, deviceId={}", processId, dto.getDeviceId());
    }

    /**
     * 提交工序质检结果：写入检验记录，不合格产品标记 REDO，全部合格时自动推进到下一工序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitProcessQc(Long processId, SubmitProcessQcDTO dto) {
        ProductionProcessEntity process = getById(processId);
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_PROCESS_NOT_FOUND);
        }
        if (!ProcessStatusEnum.COMPLETED.getCode().equals(process.getStatus())) {
            throw new BusinessException(400, "工序尚未完成，无法提交质检结果");
        }

        Long inspectorId = StpUtil.getLoginIdAsLong();
        boolean hasRedoInThisSubmit = false;
        List<Long> newlyRedoProductIds = new ArrayList<>();

        for (ProcessProductResultDTO r : dto.getProductResults()) {
            // 旧记录置为非最新
            processProductResultMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProcessProductResultEntity>()
                            .eq(ProductionProcessProductResultEntity::getProductionProcessId, processId)
                            .eq(ProductionProcessProductResultEntity::getProductionProductId, r.getProductId())
                            .set(ProductionProcessProductResultEntity::getIsLatest, 0));

            long prevCount = processProductResultMapper.selectCount(
                    new LambdaQueryWrapper<ProductionProcessProductResultEntity>()
                            .eq(ProductionProcessProductResultEntity::getProductionProcessId, processId)
                            .eq(ProductionProcessProductResultEntity::getProductionProductId, r.getProductId()));

            ProductionProcessProductResultEntity result = new ProductionProcessProductResultEntity();
            result.setProductionProcessId(processId);
            result.setProductionProductId(r.getProductId());
            result.setResult(r.getResult());
            result.setRemark(r.getRemark());
            result.setAttemptNo((int) prevCount + 1);
            result.setIsLatest(1);
            result.setInspectorId(inspectorId);
            result.setInspectTime(LocalDateTime.now());
            processProductResultMapper.insert(result);

            if (QcResultEnum.REDO.getCode().equals(r.getResult())) {
                hasRedoInThisSubmit = true;
                ProductionProductEntity product = productMapper.selectById(r.getProductId());
                if (product != null) {
                    product.setStatus(ProductStatusEnum.REDO.getCode());
                    product.setRedoProcessType(process.getProcessType());
                    productMapper.updateById(product);
                    newlyRedoProductIds.add(r.getProductId());
                }
            } else {
                // pass：若产品之前是 REDO 状态（重做后通过），恢复为 IN_PROCESS
                productMapper.update(null,
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProductEntity>()
                                .eq(ProductionProductEntity::getId, r.getProductId())
                                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode())
                                .set(ProductionProductEntity::getStatus, ProductStatusEnum.IN_PROCESS.getCode())
                                .set(ProductionProductEntity::getRedoProcessType, null));
            }
        }

        if (hasRedoInThisSubmit) {
            recordMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                            .eq(ProductionRecordEntity::getId, process.getProductionRecordId())
                            .set(ProductionRecordEntity::getHasRedoProduct, 1));
        }

        // 无新增 redo 时尝试推进（tryAdvanceProcess 内部做全量 redo 校验）
        if (!hasRedoInThisSubmit) {
            tryAdvanceProcess(process);
        }

        log.info("提交工序质检结果: processId={}, processType={}, total={}, hasRedo={}",
                processId, process.getProcessType(), dto.getProductResults().size(), hasRedoInThisSubmit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handlePrintFailure(Long recordId, String failureReason, boolean recreate) {
        return handlePrintAbandon(recordId, failureReason, recreate, FlowStatusEnum.PRINT_FAILED, "打印失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handlePrintInspectionFail(Long recordId, String failureReason, boolean recreate) {
        return handlePrintAbandon(recordId, failureReason, recreate, FlowStatusEnum.CANCELLED, "打印检验不合格");
    }

    private Long handlePrintAbandon(Long recordId, String failureReason, boolean recreate,
                                    FlowStatusEnum abandonStatus, String logPrefix) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (!recreate) {
            log.info("{}-修复后继续: recordId={}, recordNo={}, reason={}", logPrefix, recordId, record.getRecordNo(), failureReason);
            return null;
        }
        record.setStatus(abandonStatus.getValue());
        recordMapper.updateById(record);
        List<Long> productIds = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId)
                        .select(ProductionProductEntity::getId))
                .stream().map(ProductionProductEntity::getId).collect(Collectors.toList());
        if (!productIds.isEmpty()) {
            productMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProductEntity>()
                    .in(ProductionProductEntity::getId, productIds)
                    .set(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode()));
        }
        log.info("{}-废弃流转卡: recordId={}, recordNo={}, reason={}, voidedProductCount={}",
                logPrefix, recordId, record.getRecordNo(), failureReason, productIds.size());
        return null;
    }

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
        // PENDING：首次开始；COMPLETED：redo 重做后再次开始
        if (!ProcessStatusEnum.PENDING.getCode().equals(process.getStatus())
                && !ProcessStatusEnum.COMPLETED.getCode().equals(process.getStatus())) {
            throw new BusinessException(400, "工序进行中，无法重复开始");
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
     * 完成工序：标记工序为已完成，记录结束时间。
     * 打印工序同时推进流转卡状态；后处理工序需后续调用 submitProcessQc 提交质检结果才推进。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishProcess(Long recordId, String processType) {
        ProductionProcessEntity process = getOne(new LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, processType));
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
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

        if (ProcessTypeEnum.PRINT.getCode().equals(processType)) {
            ProductionRecordEntity record = recordMapper.selectById(recordId);
            if (record == null) return;
            record.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        }
        log.info("完成工序: recordId={}, processType={}", recordId, processType);
    }

    private void tryAdvanceProcess(ProductionProcessEntity process) {
        long redoCount = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode()));
        if (redoCount > 0) {
            return;
        }
        // 所有 redo 已清零，同步流转卡标志
        recordMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                        .eq(ProductionRecordEntity::getId, process.getProductionRecordId())
                        .set(ProductionRecordEntity::getHasRedoProduct, 0));

        ProductionRecordEntity record = recordMapper.selectById(process.getProductionRecordId());
        if (record == null) return;

        String processType = process.getProcessType();
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
        log.info("工序推进: recordId={}, processType={}", process.getProductionRecordId(), processType);
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
