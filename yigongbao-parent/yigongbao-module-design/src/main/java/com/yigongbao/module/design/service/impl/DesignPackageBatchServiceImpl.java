package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.event.DesignCompletedEvent;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.module.design.entity.DesignPackageBatchEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.enums.DesignPackageBatchStatus;
import com.yigongbao.module.design.mapper.DesignPackageBatchMapper;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.service.DesignPackageBatchService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
            2030, 3010, 3020, 3030, 3040, 4010, 5010, 5020, 5030, 5040,
            5050, 6010, 6020, 6030, 8010);

    private final OrderMainService orderMainService;
    private final DesignPackageService packageService;
    private final DesignProductService productService;
    private final ApplicationEventPublisher eventPublisher;
    private final FlowFacade flowFacade;
    private final UserService userService;

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
        DesignPackageBatchEntity unfinished = getOne(new LambdaQueryWrapper<DesignPackageBatchEntity>()
                .eq(DesignPackageBatchEntity::getOrderId, orderId)
                .in(DesignPackageBatchEntity::getStatus,
                        DesignPackageBatchStatus.UPLOADING.name(), DesignPackageBatchStatus.PRINT_INFO_EDITING.name())
                .orderByDesc(DesignPackageBatchEntity::getCreateTime)
                .last("LIMIT 1"), false);
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
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId, Long batchId) {
        DesignPackageBatchEntity batch = getOwnedBatch(orderId, batchId);
        if (DesignPackageBatchStatus.COMPLETED.name().equals(batch.getStatus())
                || DesignPackageBatchStatus.CANCELLED.name().equals(batch.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }

        List<DesignPackageEntity> packages = packageService.list(new LambdaQueryWrapper<DesignPackageEntity>()
                .eq(DesignPackageEntity::getOrderId, orderId)
                .eq(DesignPackageEntity::getBatchId, batchId)
                .orderByAsc(DesignPackageEntity::getPackageSeq));
        if (packages.isEmpty() || packages.stream()
                .anyMatch(pkg -> productService.countByPackageId(pkg.getId()) <= 0)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED,
                    "追加批次中的所有数据包必须完成打印信息填写");
        }

        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!ALLOWED_ORDER_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        UserEntity operatorUser = userService.getById(operatorId);
        String operatorName = operatorUser == null ? null : operatorUser.getRealName();

        // 先锁定批次；订单版本条件更新仍是最终并发闸门，失败时整个事务回滚。
        batch.setStatus(DesignPackageBatchStatus.COMPLETED.name());
        batch.setCompletedTime(LocalDateTime.now());
        batchServiceUpdate(batch);

        flowFacade.executeAdditionalDesignBatchComplete(
                orderId,
                new FlowOperator(operatorId, operatorName, "批次=" + batch.getBatchNo()),
                order.getVersion());
        eventPublisher.publishEvent(new DesignCompletedEvent(this, orderId,
                packages.stream().map(DesignPackageEntity::getId).toList()));
    }

    private DesignPackageBatchEntity getOwnedBatch(Long orderId, Long batchId) {
        DesignPackageBatchEntity batch = getOne(new LambdaQueryWrapper<DesignPackageBatchEntity>()
                .eq(DesignPackageBatchEntity::getId, batchId)
                .eq(DesignPackageBatchEntity::getOrderId, orderId)
                .last("FOR UPDATE"), false);
        if (batch == null || !orderId.equals(batch.getOrderId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        return batch;
    }

    private void batchServiceUpdate(DesignPackageBatchEntity batch) {
        if (!updateById(batch)) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "追加批次状态更新失败");
        }
    }
}
