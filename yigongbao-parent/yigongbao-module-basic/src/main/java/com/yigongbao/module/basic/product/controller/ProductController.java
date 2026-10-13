package com.yigongbao.module.basic.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.CreateProductSpecDTO;
import com.yigongbao.module.basic.product.dto.ProductCategoryDTO;
import com.yigongbao.module.basic.product.dto.ProductListDTO;
import com.yigongbao.module.basic.product.dto.ProductPageDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductSpecDTO;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.service.ProductSpecService;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
import com.yigongbao.module.basic.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品型号管理 Controller
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Tag(name = "产品型号管理", description = "产品型号信息管理")
@RestController
@RequestMapping("/basic/product")
@RequiredArgsConstructor
@RequireSign
@Validated
public class ProductController {

    private final ProductService productService;
    private final ProductSpecService productSpecService;

    // ==================== 产品接口 ====================

    /**
     * 分页查询产品列表
     */
    @Operation(summary = "分页查询产品列表")
    @PostMapping("/page")
    public Result<IPage<ProductVO>> page(@Validated @RequestBody ProductPageDTO dto) {
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        return Result.success(productService.listProducts(pageNum, pageSize, dto.getProductName(),
                dto.getCategory(), null, dto.getStatus()));
    }

    /**
     * 查询所有产品列表
     */
    @Operation(summary = "查询所有产品列表")
    @PostMapping("/list")
    public Result<List<ProductVO>> list(@Validated @RequestBody ProductListDTO dto) {
        return Result.success(productService.listAll(dto.getProductName(), dto.getCategory(), dto.getStatus()));
    }

    /**
     * 根据ID查询产品详情（含 specs 列表）
     */
    @Operation(summary = "根据ID查询产品详情（含规格列表）")
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /**
     * 创建产品
     */
    @Operation(summary = "创建产品")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建产品"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateProductDTO dto) {
        productService.create(dto);
        return Result.success();
    }

    /**
     * 更新产品
     */
    @Operation(summary = "更新产品")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新产品"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateProductDTO dto) {
        productService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除产品（有规格时拒绝）
     */
    @Operation(summary = "删除产品")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除产品"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        productService.remove(id);
        return Result.success();
    }

    /**
     * 按分类查询产品（含 specs 列表）
     */
    @Operation(summary = "按分类查询产品（含规格）")
    @PostMapping("/list-by-category")
    public Result<List<ProductVO>> listByCategory(@Validated @RequestBody ProductCategoryDTO dto) {
        return Result.success(productService.listByCategory(dto.getCategory()));
    }

    // ==================== 规格接口 ====================

    /**
     * 创建规格
     */
    @Operation(summary = "创建产品规格")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建产品规格"
    )
    @PostMapping("/{id}/spec")
    public Result<Void> createSpec(@PathVariable Long id,
                                   @Validated @RequestBody CreateProductSpecDTO dto) {
        productSpecService.create(id, dto);
        return Result.success();
    }

    /**
     * 查询规格列表
     */
    @Operation(summary = "查询产品规格列表")
    @GetMapping("/{id}/specs")
    public Result<List<ProductSpecVO>> listSpecs(@PathVariable Long id) {
        return Result.success(productSpecService.listByProductId(id));
    }

    /**
     * 更新规格
     */
    @Operation(summary = "更新产品规格")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新产品规格"
    )
    @PutMapping("/spec/{specId}")
    public Result<Void> updateSpec(@PathVariable Long specId,
                                   @Validated @RequestBody UpdateProductSpecDTO dto) {
        productSpecService.update(specId, dto);
        return Result.success();
    }

    /**
     * 删除规格（被引用时拒绝）
     */
    @Operation(summary = "删除产品规格")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除产品规格"
    )
    @DeleteMapping("/spec/{specId}")
    public Result<Void> removeSpec(@PathVariable Long specId) {
        productSpecService.remove(specId);
        return Result.success();
    }
}
