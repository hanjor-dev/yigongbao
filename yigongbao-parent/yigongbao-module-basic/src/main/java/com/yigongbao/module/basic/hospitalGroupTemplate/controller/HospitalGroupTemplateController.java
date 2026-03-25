package com.yigongbao.module.basic.hospitalGroupTemplate.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 医院组合模板 Controller
 * 处理医院组合模板相关的 HTTP 请求，包括 CRUD 和状态管理
 *
 * @author hanjor
 * @date 2026-03-19
 */
@RestController
@RequestMapping("/api/basic/hospital-group-template")
@RequiredArgsConstructor
@Validated
public class HospitalGroupTemplateController {

    private final HospitalGroupTemplateService templateService;

    @GetMapping("/list")
    public Result<IPage<HospitalGroupTemplateVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(templateService.listTemplate(pageNum, pageSize, templateName, status));
    }

    @GetMapping("/{id}")
    public Result<HospitalGroupTemplateVO> getById(@PathVariable Long id) {
        return Result.success(templateService.getTemplateById(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateHospitalGroupTemplateDTO dto) {
        templateService.createTemplate(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateHospitalGroupTemplateDTO dto) {
        templateService.updateTemplate(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        templateService.removeTemplate(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {
        templateService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/options")
    public Result<List<HospitalGroupTemplateSimpleVO>> options(
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(templateService.listOptions(status));
    }
}
