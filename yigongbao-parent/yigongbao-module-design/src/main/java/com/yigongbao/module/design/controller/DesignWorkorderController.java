package com.yigongbao.module.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设计工单查询 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "设计工单", description = "设计工单列表查询与详情")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignWorkorderController {

    private final DesignWorkorderService designWorkorderService;

    /**
     * 分页查询设计工单列表
     */
    @Operation(summary = "分页查询设计工单列表")
    @PostMapping("/list")
    public Result<IPage<DesignWorkorderListVO>> listWorkorders(@Validated @RequestBody DesignWorkorderQueryDTO queryDTO) {
        return Result.success(designWorkorderService.listWorkorders(queryDTO));
    }

    /**
     * 获取设计工单详情
     */
    @Operation(summary = "获取设计工单详情")
    @GetMapping("/{orderId}")
    public Result<DesignWorkorderDetailVO> getWorkorderDetail(@PathVariable Long orderId) {
        return Result.success(designWorkorderService.getWorkorderDetail(orderId));
    }
}
