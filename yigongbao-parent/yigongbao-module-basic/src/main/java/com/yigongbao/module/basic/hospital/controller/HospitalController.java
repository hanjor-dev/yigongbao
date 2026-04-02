package com.yigongbao.module.basic.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.dto.HospitalPageDTO;
import com.yigongbao.module.basic.hospital.dto.UpdateHospitalDTO;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
 * 医院管理 Controller
 * 处理医院相关的 HTTP 请求，包括 CRUD 和状态管理
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Tag(name = "医院管理", description = "医院信息 CRUD、状态管理、下拉选项")
@RestController
@RequestMapping("/basic/hospital")
@RequiredArgsConstructor
@Validated
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * 分页查询医院列表
     */
    @Operation(summary = "分页查询医院列表")
    @PostMapping("/list")
    public Result<IPage<HospitalVO>> list(@Validated @RequestBody HospitalPageDTO dto) {
        return Result.success(hospitalService.listHospital(dto));
    }

    /**
     * 根据ID查询医院详情
     */
    @Operation(summary = "根据ID查询医院详情")
    @GetMapping("/{id}")
    public Result<HospitalVO> getById(@PathVariable Long id) {
        return Result.success(hospitalService.getHospitalById(id));
    }

    /**
     * 创建医院
     */
    @Operation(summary = "创建医院")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建医院"
    )
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateHospitalDTO dto) {
        hospitalService.createHospital(dto);
        return Result.success();
    }

    /**
     * 更新医院
     */
    @Operation(summary = "更新医院")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新医院"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateHospitalDTO dto) {
        hospitalService.updateHospital(id, dto);
        return Result.success();
    }

    /**
     * 修改医院状态
     */
    @Operation(summary = "修改医院状态")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改医院状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {
        hospitalService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取医院下拉选项
     */
    @Operation(summary = "获取医院下拉选项")
    @GetMapping("/options")
    public Result<List<HospitalVO>> options(@RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(hospitalService.listOptions(status));
    }
}
