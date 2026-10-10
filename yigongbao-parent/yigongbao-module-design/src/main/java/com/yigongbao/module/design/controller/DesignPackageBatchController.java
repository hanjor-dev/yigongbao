package com.yigongbao.module.design.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.result.Result;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.design.entity.DesignPackageBatchEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.enums.DesignPackageBatchStatus;
import com.yigongbao.module.design.service.DesignPackageBatchService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.DesignPackageBatchVO;
import com.yigongbao.module.order.service.OrderMainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "追加设计批次")
@RestController
@RequestMapping("/design/package-batch")
@RequiredArgsConstructor
public class DesignPackageBatchController {

    private static final List<Integer> ALLOWED_STATUSES = List.of(
            2020, 2030, 3010, 3020, 3030, 3040, 4010, 5010, 5020, 5030, 5040,
            5050, 6010, 6020, 6030, 8010);

    private final OrderMainService orderMainService;
    private final DesignPackageBatchService batchService;
    private final DesignPackageService packageService;
    private final DesignProductService productService;
    private final com.yigongbao.module.design.helper.DesignQueryHelper designQueryHelper;

    @Operation(summary = "创建追加设计批次")
    @PostMapping
    public Result<DesignPackageBatchVO> create(@RequestParam Long orderId) {
        OrderMainEntity order = checkAllowedOrder(orderId);
        designQueryHelper.checkIsAssignedDesigner(order);

        return Result.success(toVO(batchService.createOrReuse(orderId)));
    }

    @Operation(summary = "查询追加设计批次")
    @GetMapping("/{batchId}")
    public Result<DesignPackageBatchVO> get(@PathVariable Long batchId, @RequestParam Long orderId) {
        designQueryHelper.checkOrderReadable(orderId);
        DesignPackageBatchEntity entity = getOwnedBatch(orderId, batchId);
        return Result.success(toVO(entity));
    }

    @Operation(summary = "查询订单追加设计批次")
    @GetMapping
    public Result<List<DesignPackageBatchVO>> list(@RequestParam Long orderId) {
        designQueryHelper.checkOrderReadable(orderId);
        return Result.success(batchService.list(new LambdaQueryWrapper<DesignPackageBatchEntity>()
                        .eq(DesignPackageBatchEntity::getOrderId, orderId)
                        .orderByDesc(DesignPackageBatchEntity::getCreateTime))
                .stream().map(this::toVO).toList());
    }

    private OrderMainEntity checkAllowedOrder(Long orderId) {
        orderMainService.checkNotClassicCase(orderId, "创建追加设计批次");
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!ALLOWED_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        return order;
    }

    private DesignPackageBatchEntity getOwnedBatch(Long orderId, Long batchId) {
        DesignPackageBatchEntity entity = batchService.getById(batchId);
        if (entity == null || !orderId.equals(entity.getOrderId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        return entity;
    }

    private DesignPackageBatchVO toVO(DesignPackageBatchEntity entity) {
        List<DesignPackageEntity> packages = packageService.list(new LambdaQueryWrapper<DesignPackageEntity>()
                .eq(DesignPackageEntity::getOrderId, entity.getOrderId())
                .eq(DesignPackageEntity::getBatchId, entity.getId()));
        long completed = packages.stream().filter(pkg -> productService.lambdaQuery()
                .eq(com.yigongbao.module.design.entity.DesignProductEntity::getPackageId, pkg.getId()).exists()).count();
        DesignPackageBatchVO vo = new DesignPackageBatchVO();
        vo.setBatchId(entity.getId());
        vo.setBatchNo(entity.getBatchNo());
        vo.setOrderId(entity.getOrderId());
        vo.setStatus(entity.getStatus());
        vo.setSourceOrderStatus(entity.getSourceOrderStatus());
        vo.setPackageCount(packages.size());
        vo.setCompletedPackageCount((int) completed);
        vo.setCanEdit(!DesignPackageBatchStatus.COMPLETED.name().equals(entity.getStatus())
                && !DesignPackageBatchStatus.CANCELLED.name().equals(entity.getStatus()));
        vo.setCreateTime(entity.getCreateTime());
        vo.setCompletedTime(entity.getCompletedTime());
        return vo;
    }
}
