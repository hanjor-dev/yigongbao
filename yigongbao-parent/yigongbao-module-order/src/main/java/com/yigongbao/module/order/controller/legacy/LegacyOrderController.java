package com.yigongbao.module.order.controller.legacy;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.order.dto.legacy.LegacyOrderPageDTO;
import com.yigongbao.module.order.dto.legacy.LegacyMigrationTaskPageDTO;
import com.yigongbao.module.order.entity.legacy.LegacyMigrationTaskEntity;
import com.yigongbao.module.order.service.legacy.LegacyOrderMigrationService;
import com.yigongbao.module.order.service.legacy.LegacyOrderQueryService;
import com.yigongbao.module.order.vo.legacy.LegacyOrderListVO;
import com.yigongbao.module.order.vo.legacy.LegacyOrderFilterOptionsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/legacy-order")
@RequiredArgsConstructor
@RequireSign
@Tag(name = "历史订单")
public class LegacyOrderController {
    private final LegacyOrderQueryService queryService;
    private final LegacyOrderMigrationService migrationService;

    @PostMapping("/page")
    @RequirePermission("LegacyOrder")
    @Operation(summary = "分页查询历史订单")
    public Result<IPage<LegacyOrderListVO>> page(@Valid @RequestBody LegacyOrderPageDTO dto) {
        return Result.success(queryService.page(dto));
    }

    @GetMapping("/filter-options")
    @RequirePermission("LegacyOrder")
    @Operation(summary = "查询历史订单筛选项")
    public Result<LegacyOrderFilterOptionsVO> filterOptions() {
        return Result.success(queryService.filterOptions());
    }

    @PostMapping("/migration/incremental")
    @RequirePermission("legacyOrder:Migrate")
    @OperationLog(module = "历史订单", businessType = OperationTypeEnum.IMPORT, operation = "执行历史订单增量迁移")
    @Operation(summary = "触发历史订单增量迁移")
    public Result<Long> migrate() {
        return Result.success(migrationService.createIncrementalTask(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/migration/tasks/{taskId}")
    @RequirePermission("LegacyOrder")
    @Operation(summary = "查询历史订单迁移任务")
    public Result<LegacyMigrationTaskEntity> task(@PathVariable Long taskId) {
        return Result.success(migrationService.getTask(taskId));
    }

    @GetMapping("/migration/tasks/latest")
    @RequirePermission("LegacyOrder")
    @Operation(summary = "查询最近历史订单迁移任务")
    public Result<LegacyMigrationTaskEntity> latestTask() {
        return Result.success(migrationService.getLatestTask());
    }

    @PostMapping("/migration/tasks/page")
    @RequirePermission("LegacyOrder")
    @Operation(summary = "分页查询历史订单迁移任务")
    public Result<IPage<LegacyMigrationTaskEntity>> taskPage(@Valid @RequestBody LegacyMigrationTaskPageDTO dto) {
        return Result.success(migrationService.pageTasks(dto));
    }
}
