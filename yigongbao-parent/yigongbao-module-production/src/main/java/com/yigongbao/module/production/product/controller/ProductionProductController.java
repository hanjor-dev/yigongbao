package com.yigongbao.module.production.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.product.dto.ProductionProductPageDTO;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.module.production.product.vo.ProductionProductDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 生产产品明细管理
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Tag(name = "生产产品明细管理")
@RestController
@RequestMapping("/production/product")
@RequiredArgsConstructor
public class ProductionProductController {

    private final IProductionProductService productService;

    @Operation(summary = "分页查询产品明细列表")
    @PostMapping("/list")
    public Result<IPage<ProductionProductDetailVO>> list(@RequestBody ProductionProductPageDTO dto) {
        return Result.success(productService.pageProductDetails(dto));
    }
}
