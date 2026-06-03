package com.yigongbao.module.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IOrderClassicCaseService;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单经典案例Controller
 */
@RestController
@RequestMapping("/order/classic-case")
@RequiredArgsConstructor
public class OrderClassicCaseController {

    private final IOrderClassicCaseService classicCaseService;

    @PostMapping("/mark")
    public Result<Void> markAsClassicCase(@RequestBody @Validated MarkClassicCaseDTO dto) {
        classicCaseService.markAsClassicCase(dto);
        return Result.success();
    }

    @PostMapping("/list")
    public Result<IPage<ClassicCaseVO>> list(@RequestBody ClassicCaseQueryDTO dto) {
        return Result.success(classicCaseService.listClassicCases(dto));
    }

    @GetMapping("/{orderId}")
    public Result<ClassicCaseVO> detail(@PathVariable Long orderId) {
        return Result.success(classicCaseService.getClassicCaseDetail(orderId));
    }
}
