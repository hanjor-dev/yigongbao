package com.yigongbao.module.production.process.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.transfer.entity.ProductionProcessTransferEntity;
import com.yigongbao.module.production.transfer.mapper.ProductionProcessTransferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final ProductionProcessTransferMapper transferMapper;
    private final IProductionRecordService recordService;

    /**
     * 填写工序信息，完成后检查是否有 redo 产品在此工序重做，自动恢复为 in_process
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillProcess(Long processId, FillProcessDTO dto) {
        ProductionProcessEntity process = getById(processId);
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        process.setDeviceId(dto.getDeviceId());
        process.setProcessParams(dto.getProcessParams());
        process.setHasRedo(dto.getHasRedo());
        process.setRedoRemark(dto.getRedoRemark());
        process.setStatus(ProcessStatusEnum.COMPLETED.getCode());
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
            // 若无剩余 redo 产品，清除流转卡的 hasRedoProduct 标记
            long remainRedo = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                    .eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
                    .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode()));
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

    /**
     * 工序流转：记录流转日志，更新流转卡状态，并聚合判断是否触发 Flow
     * PRINT完成 → print_completed → 聚合触发 COMPLETE_PRINT
     * WASH/CURE完成 → post_processing（更新 current_process）
     * CLEAN_DRY完成 → qc_in_progress → 聚合触发 COMPLETE_POST_PROCESSING
     * PACK完成 → 由 ProductionPackServiceImpl.transferToWarehouse() 处理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToNext(Long recordId, String fromProcess, String toProcess) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        // 记录流转日志
        ProductionProcessTransferEntity transfer = new ProductionProcessTransferEntity();
        transfer.setProductionRecordId(recordId);
        transfer.setFromProcessType(fromProcess);
        transfer.setToProcessType(toProcess);
        transfer.setTransferTime(LocalDateTime.now());
        transfer.setScanUserId(StpUtil.getLoginIdAsLong());
        transfer.setScanUserName(StpUtil.getSession().get("username", "").toString());
        transferMapper.insert(transfer);

        if (ProcessTypeEnum.PRINT.getCode().equals(fromProcess)) {
            record.setStatus(RecordStatusEnum.PRINT_COMPLETED.getCode());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    RecordStatusEnum.PRINT_COMPLETED.getCode(), FlowActionEnum.COMPLETE_PRINT);
        } else if (ProcessTypeEnum.WASH.getCode().equals(fromProcess)
                || ProcessTypeEnum.CURE.getCode().equals(fromProcess)) {
            record.setStatus(RecordStatusEnum.POST_PROCESSING.getCode());
            record.setCurrentProcess(toProcess);
            recordMapper.updateById(record);
        } else if (ProcessTypeEnum.CLEAN_DRY.getCode().equals(fromProcess)) {
            record.setStatus(RecordStatusEnum.QC_IN_PROGRESS.getCode());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    RecordStatusEnum.QC_IN_PROGRESS.getCode(), FlowActionEnum.COMPLETE_POST_PROCESSING);
        }

        log.info("工序流转: recordId={}, recordNo={}, {} -> {}, scanUser={}",
                recordId, record.getRecordNo(), fromProcess, toProcess, transfer.getScanUserName());
    }

    /**
     * 打印失败处理
     * recreate=false：修复后继续，仅记录日志
     * recreate=true：废弃原流转卡（print_failed），逻辑删除产品记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handlePrintFailure(Long recordId, String failureReason, boolean recreate) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (!recreate) {
            log.info("打印失败-修复后继续: recordId={}, recordNo={}, reason={}", recordId, record.getRecordNo(), failureReason);
            return null;
        }
        record.setStatus(RecordStatusEnum.PRINT_FAILED.getCode());
        recordMapper.updateById(record);
        List<ProductionProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId));
        products.forEach(p -> productMapper.deleteById(p.getId()));
        log.info("打印失败-废弃流转卡: recordId={}, recordNo={}, reason={}, voidedProductCount={}",
                recordId, record.getRecordNo(), failureReason, products.size());
        return null;
    }

    /**
     * 打印检验不合格处理
     * recreate=false：重打后继续，仅记录日志
     * recreate=true：废弃原流转卡（abandoned），逻辑删除产品记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long handlePrintInspectionFail(Long recordId, String failureReason, boolean recreate) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (!recreate) {
            log.info("打印检验不合格-重打后继续: recordId={}, recordNo={}, reason={}", recordId, record.getRecordNo(), failureReason);
            return null;
        }
        record.setStatus(RecordStatusEnum.ABANDONED.getCode());
        recordMapper.updateById(record);
        List<ProductionProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId));
        products.forEach(p -> productMapper.deleteById(p.getId()));
        log.info("打印检验不合格-废弃流转卡: recordId={}, recordNo={}, reason={}, voidedProductCount={}",
                recordId, record.getRecordNo(), failureReason, products.size());
        return null;
    }
}
