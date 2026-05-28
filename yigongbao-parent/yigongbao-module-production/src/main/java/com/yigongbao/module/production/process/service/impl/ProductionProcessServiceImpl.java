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
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
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

    /**
     * 填写工序信息，完成后检查是否有 redo 产品在此工序重做，自动恢复为 in_process
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
        process.setStatus(ProcessStatusEnum.COMPLETED.getCode());
        process.setEndTime(LocalDateTime.now());
        updateById(process);

        // redo 产品重做完成后自动恢复为 in_process
        List<ProductionProductEntity> redoProducts = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
                        .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode())
                        .eq(ProductionProductEntity::getRedoProcessType, process.getProcessType()));
        if (!redoProducts.isEmpty()) {
            redoProducts.forEach(p -> {
                p.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
                p.setRedoProcessType(null);
                productMapper.updateById(p);
            });
            // 用已恢复的列表直接判断，避免再次查库
            long remainRedo = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                    .eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
                    .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode())
                    .notIn(ProductionProductEntity::getId,
                            redoProducts.stream().map(ProductionProductEntity::getId).collect(Collectors.toList())));
            if (remainRedo == 0) {
                ProductionRecordEntity record = new ProductionRecordEntity();
                record.setId(process.getProductionRecordId());
                record.setHasRedoProduct(0);
                recordMapper.updateById(record);
            }
            log.info("redo产品重做完成，状态恢复为in_process: processId={}, processType={}, productCount={}",
                    processId, process.getProcessType(), redoProducts.size());
        }
        log.info("填写工序信息: processId={}, deviceId={}", processId, dto.getDeviceId());
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
        // 校验工序状态：只有 PENDING 状态才能开始
        if (!ProcessStatusEnum.PENDING.getCode().equals(process.getStatus())) {
            throw new BusinessException(400, "工序已开始或已完成，无法重复开始");
        }
        process.setDeviceId(dto.getPrimaryDeviceId());
        DeviceEntity device = deviceMapper.selectById(dto.getPrimaryDeviceId());
        if (device != null) {
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
        // 仅后处理工序更新流转卡状态
        if (!ProcessTypeEnum.PRINT.getCode().equals(dto.getProcessType())) {
            record.setStatus(FlowStatusEnum.POST_PROCESSING.getValue());
            record.setCurrentProcess(dto.getProcessType());
            recordMapper.updateById(record);
        }
        log.info("开始工序: recordId={}, processType={}, deviceId={}", recordId, dto.getProcessType(), dto.getPrimaryDeviceId());
    }

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
        process.setEndTime(LocalDateTime.now());
        process.setStatus(ProcessStatusEnum.COMPLETED.getCode());
        updateById(process);

        // 完成工序后自动推进流转卡状态
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            return;
        }
        if (ProcessTypeEnum.PRINT.getCode().equals(processType)) {
            record.setStatus(FlowStatusEnum.PRINT_COMPLETED.getValue());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
        } else if (ProcessTypeEnum.WASH.getCode().equals(processType)) {
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
        log.info("完成工序并自动流转: recordId={}, processType={}", recordId, processType);
    }
}
