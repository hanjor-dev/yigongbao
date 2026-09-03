package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.service.ProductionPrintLifecycleService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionPrintLifecycleServiceImpl implements ProductionPrintLifecycleService {

    private final ProductionRecordMapper recordMapper;
    private final ProductionProcessMapper processMapper;
    private final IProductionProcessService processService;
    private final IProductionRecordService recordService;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceCompletePrint(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectByIdForUpdate(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        UserEntity user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null || !RoleCodeEnum.PRODUCTION_MANAGER.getCode().equals(user.getRoleCode())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
        if (user.getCenterId() == null || !Objects.equals(user.getCenterId(), record.getProcessingCenterId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
        if (FlowStatusEnum.PRINT_COMPLETED.getValue().equals(record.getStatus())) {
            return;
        }
        if (!FlowStatusEnum.PRINTING.getValue().equals(record.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL,
                    "只有打印中的流转卡才能强制完成打印");
        }
        completePrint(recordId, "manual-force");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completePrint(Long recordId, String source) {
        ProductionRecordEntity record = recordMapper.selectByIdForUpdate(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (!FlowStatusEnum.PRINTING.getValue().equals(record.getStatus())) {
            log.info("打印完成跳过非打印中流转卡: recordId={}, status={}, source={}",
                    recordId, record.getStatus(), source);
            return false;
        }

        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime printFinishTime = record.getPrintFinishTime() == null
                ? now : record.getPrintFinishTime().withNano(0);
        LambdaUpdateWrapper<ProductionRecordEntity> wrapper = new LambdaUpdateWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getId, recordId)
                .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINTING.getValue())
                .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PRINT_COMPLETED.getValue())
                .set(ProductionRecordEntity::getCurrentProcess, null)
                .set(ProductionRecordEntity::getContentUpdateTime, now);
        if (record.getPrintFinishTime() == null) {
            wrapper.set(ProductionRecordEntity::getPrintFinishTime, printFinishTime);
        }
        if (recordMapper.update(null, wrapper) == 0) {
            return false;
        }

        processService.schedulePostProcessing(recordId, printFinishTime);
        int processUpdated = processMapper.update(null, new LambdaUpdateWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode())
                .set(ProductionProcessEntity::getStatus, ProcessStatusEnum.COMPLETED.getCode())
                .set(ProductionProcessEntity::getEndTime, printFinishTime));
        if (processUpdated != 1) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_ABNORMAL,
                    "打印工序记录缺失或重复，无法完成打印");
        }

        if (record.getOrderId() != null) {
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT);
            recordService.reconcileOrderProductionStatus(record.getOrderId());
        }
        log.info("打印完成: recordId={}, recordNo={}, orderId={}, source={}",
                recordId, record.getRecordNo(), record.getOrderId(), source);
        return true;
    }
}
