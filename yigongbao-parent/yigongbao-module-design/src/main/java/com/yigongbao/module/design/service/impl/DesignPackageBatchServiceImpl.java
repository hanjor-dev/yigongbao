package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.design.entity.DesignPackageBatchEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.enums.DesignPackageBatchStatus;
import com.yigongbao.module.design.mapper.DesignPackageBatchMapper;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignPackageBatchService;
import com.yigongbao.module.order.service.OrderMainService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignPackageBatchServiceImpl
        extends ServiceImpl<DesignPackageBatchMapper, DesignPackageBatchEntity>
        implements DesignPackageBatchService {

    private static final List<Integer> ALLOWED_ORDER_STATUSES = List.of(
            2020, 2030, 3010, 3020, 3030, 3040, 4010, 5010, 5020, 5030, 5040,
            5050, 6010, 6020, 6030, 8010);

    private final OrderMainService orderMainService;
    private final DesignPackageService packageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignPackageBatchEntity createOrReuse(Long orderId) {
        OrderMainEntity order = orderMainService.getBaseMapper().selectOne(
                new LambdaQueryWrapper<OrderMainEntity>()
                        .eq(OrderMainEntity::getId, orderId)
                        .last("FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!ALLOWED_ORDER_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        DesignPackageBatchEntity unfinished = findUnfinished(orderId);
        if (unfinished != null) {
            return unfinished;
        }
        DesignPackageBatchEntity entity = new DesignPackageBatchEntity();
        entity.setOrderId(orderId);
        entity.setBatchNo("AD-" + orderId + "-" + System.currentTimeMillis());
        entity.setBatchType("ADDITIONAL");
        entity.setStatus(DesignPackageBatchStatus.UPLOADING.name());
        entity.setSourceOrderStatus(order.getStatus());
        entity.setCreatedBy(StpUtil.getLoginIdAsLong());
        entity.setVersion(0);
        save(entity);
        return entity;
    }

    @Override
    public DesignPackageBatchEntity findUnfinished(Long orderId) {
        return getOne(new LambdaQueryWrapper<DesignPackageBatchEntity>()
                .eq(DesignPackageBatchEntity::getOrderId, orderId)
                .in(DesignPackageBatchEntity::getStatus,
                        DesignPackageBatchStatus.UPLOADING.name(), DesignPackageBatchStatus.PRINT_INFO_EDITING.name())
                .orderByDesc(DesignPackageBatchEntity::getCreateTime)
                .last("LIMIT 1"), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> completeUnfinishedBatches(Long orderId) {
        List<DesignPackageBatchEntity> batches = list(new LambdaQueryWrapper<DesignPackageBatchEntity>()
                .eq(DesignPackageBatchEntity::getOrderId, orderId)
                .in(DesignPackageBatchEntity::getStatus,
                        DesignPackageBatchStatus.UPLOADING.name(), DesignPackageBatchStatus.PRINT_INFO_EDITING.name())
                .orderByAsc(DesignPackageBatchEntity::getCreateTime)
                .last("FOR UPDATE"));
        List<Long> packageIds = new java.util.ArrayList<>();
        for (DesignPackageBatchEntity batch : batches) {
            List<DesignPackageEntity> packages = packageService.list(new LambdaQueryWrapper<DesignPackageEntity>()
                    .eq(DesignPackageEntity::getOrderId, orderId)
                    .eq(DesignPackageEntity::getBatchId, batch.getId())
                    .orderByAsc(DesignPackageEntity::getPackageSeq));
            packageIds.addAll(packages.stream().map(DesignPackageEntity::getId).toList());
            batch.setStatus(DesignPackageBatchStatus.COMPLETED.name());
            batch.setCompletedTime(LocalDateTime.now());
            batchServiceUpdate(batch);
        }
        return packageIds;
    }

    private void batchServiceUpdate(DesignPackageBatchEntity batch) {
        if (!updateById(batch)) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "追加批次状态更新失败");
        }
    }
}
