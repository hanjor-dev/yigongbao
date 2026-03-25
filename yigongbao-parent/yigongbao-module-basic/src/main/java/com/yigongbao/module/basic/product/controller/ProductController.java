package com.yigongbao.module.basic.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.product.dto.CreateProductDTO;
import com.yigongbao.module.basic.product.dto.UpdateProductDTO;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.vo.ProductVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RestController
@RequestMapping("/api/basic/product")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    /**
     * 分页查询产品列表
     */
    @GetMapping("/page")
    public Result<IPage<ProductVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long certId,
            @RequestParam(required = false) Integer status) {
        return Result.success(productService.listProducts(pageNum, pageSize, productName, category, certId, status));
    }

    /**
     * 查询所有产品列表
     */
    @GetMapping("/list")
    public Result<List<ProductVO>> list(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        return Result.success(productService.listAll(productName, category, status));
    }

    /**
     * 根据ID查询产品
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /**
     * 创建产品
     */
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateProductDTO dto) {
        productService.create(dto);
        return Result.success();
    }

    /**
     * 更新产品
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateProductDTO dto) {
        productService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除产品
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        productService.remove(id);
        return Result.success();
    }

    /**
     * 按注册证查询产品
     */
    @GetMapping("/list-by-cert/{certId}")
    public Result<List<ProductVO>> listByCert(@PathVariable Long certId) {
        return Result.success(productService.listByCertId(certId));
    }

    /**
     * 按分类查询产品
     */
    @GetMapping("/list-by-category")
    public Result<List<ProductVO>> listByCategory(@RequestParam String category) {
        return Result.success(productService.listByCategory(category));
    }
}
