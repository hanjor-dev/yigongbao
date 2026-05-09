package com.yigongbao.module.basic.bodyPart.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.bodyPart.dto.CreateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.dto.UpdateBodyPartDTO;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 重建部位 Controller
 * 处理部位相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Tag(name = "部位管理", description = "检查部位信息管理")
@RestController
@RequestMapping("/basic/body-part")
@RequiredArgsConstructor
@RequireSign
public class BodyPartController {

    private final BodyPartService bodyPartService;

    /**
     * 获取部位列表
     */
    @Operation(summary = "获取部位列表")
    @PostMapping("/list")
    public Result<List<BodyPartVO>> list() {
        return Result.success(bodyPartService.listAll());
    }

    /**
     * 查询部位详情
     */
    @Operation(summary = "查询部位详情")
    @GetMapping("/{id}")
    public Result<BodyPartDetailVO> getById(@PathVariable Long id) {
        return Result.success(bodyPartService.getDetailById(id));
    }

    /**
     * 创建部位
     */
    @Operation(summary = "创建部位")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建部位"
    )
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateBodyPartDTO dto) {
        bodyPartService.createBodyPart(dto);
        return Result.success();
    }

    /**
     * 更新部位
     */
    @Operation(summary = "更新部位")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新部位"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateBodyPartDTO dto) {
        bodyPartService.updateBodyPart(id, dto);
        return Result.success();
    }

    /**
     * 删除部位
     */
    @Operation(summary = "删除部位")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除部位"
    )
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bodyPartService.removeBodyPart(id);
        return Result.success();
    }

    /**
     * 修改部位状态
     */
    @Operation(summary = "修改部位状态")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改部位状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull @Min(0) @Max(1) Integer status) {
        bodyPartService.updateStatus(id, status);
        return Result.success();
    }
}
