package com.yigongbao.module.basic.doctor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.basic.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.DoctorListDTO;
import com.yigongbao.module.basic.doctor.dto.DoctorPageDTO;
import com.yigongbao.module.basic.doctor.dto.DoctorSuggestDTO;
import com.yigongbao.module.basic.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.basic.doctor.service.DoctorService;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生管理 Controller
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Tag(name = "医生管理", description = "医生信息管理")
@RestController
@RequestMapping("/basic/doctor")
@RequiredArgsConstructor
@Validated
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * 分页查询医生列表
     */
    @Operation(summary = "分页查询医生列表")
    @PostMapping("/page")
    public Result<IPage<DoctorVO>> page(@Validated @RequestBody DoctorPageDTO dto) {
        return Result.success(doctorService.listDoctors(dto));
    }

    /**
     * 查询所有医生列表
     */
    @Operation(summary = "查询所有医生列表")
    @PostMapping("/list")
    public Result<List<DoctorVO>> list(@Validated @RequestBody DoctorListDTO dto) {
        return Result.success(doctorService.listAll(dto));
    }

    /**
     * 根据ID查询医生
     */
    @Operation(summary = "根据ID查询医生")
    @GetMapping("/{id}")
    public Result<DoctorVO> getById(@PathVariable Long id) {
        return Result.success(doctorService.getById(id));
    }

    /**
     * 创建医生
     */
    @Operation(summary = "创建医生")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建医生"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateDoctorDTO dto) {
        doctorService.create(dto);
        return Result.success();
    }

    /**
     * 更新医生
     */
    @Operation(summary = "更新医生")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新医生"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateDoctorDTO dto) {
        doctorService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除医生
     */
    @Operation(summary = "删除医生")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除医生"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        doctorService.remove(id);
        return Result.success();
    }

    /**
     * 修改状态
     */
    @Operation(summary = "修改状态")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改医生状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        doctorService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 查询业务员在医院下的历史医生列表（用于医生联想）
     */
    @Operation(summary = "查询业务员在医院下的历史医生列表")
    @PostMapping("/suggest")
    public Result<List<DoctorVO>> suggest(@Validated @RequestBody DoctorSuggestDTO dto) {
        return Result.success(doctorService.listByCreatorAndHospital(dto));
    }

    /**
     * 快速添加医生（订单创建时调用）
     */
    @Operation(summary = "快速添加医生")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "快速添加医生"
    )
    @PostMapping("/quick-add")
    public Result<DoctorVO> quickAdd(@Validated @RequestBody QuickAddDoctorDTO dto) {
        return Result.success(doctorService.quickAdd(dto));
    }
}
