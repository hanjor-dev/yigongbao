package com.yigongbao.module.system.hospitalGroupTemplate.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.HospitalGroupTemplatePageDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.system.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医院组合模板 Controller
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Tag(name = "医院组合模板管理", description = "医院检查项目组合模板管理")
@RestController
@RequestMapping("/system/hospital-group-template")
@RequiredArgsConstructor
@RequireSign
@Validated
public class HospitalGroupTemplateController {

    private final HospitalGroupTemplateService templateService;

    @Operation(summary = "分页查询医院组合模板列表")
    @PostMapping("/list")
    public Result<IPage<HospitalGroupTemplateVO>> list(@Validated @RequestBody HospitalGroupTemplatePageDTO dto) {
        return Result.success(templateService.listTemplate(dto));
    }

    @Operation(summary = "根据ID查询医院组合模板")
    @GetMapping("/{id}")
    public Result<HospitalGroupTemplateVO> getById(@PathVariable Long id) {
        // 模板管理场景：userId=null，assigned 表示全系统任意用户是否已分配
        return Result.success(templateService.getTemplateById(id, null));
    }

    @Operation(summary = "创建医院组合模板")
    @OperationLog(module = "系统管理", businessType = OperationTypeEnum.CREATE, operation = "创建医院组合模板")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateHospitalGroupTemplateDTO dto) {
        templateService.createTemplate(dto);
        return Result.success();
    }

    @Operation(summary = "更新医院组合模板")
    @OperationLog(module = "系统管理", businessType = OperationTypeEnum.UPDATE, operation = "更新医院组合模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateHospitalGroupTemplateDTO dto) {
        templateService.updateTemplate(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除医院组合模板")
    @OperationLog(module = "系统管理", businessType = OperationTypeEnum.DELETE, operation = "删除医院组合模板")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        templateService.removeTemplate(id);
        return Result.success();
    }

    @Operation(summary = "修改医院组合模板状态")
    @OperationLog(module = "系统管理", businessType = OperationTypeEnum.UPDATE, operation = "修改医院组合模板状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {
        templateService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "获取医院组合模板下拉选项")
    @GetMapping("/options")
    public Result<List<HospitalGroupTemplateSimpleVO>> options(
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(templateService.listOptions(status));
    }
}
