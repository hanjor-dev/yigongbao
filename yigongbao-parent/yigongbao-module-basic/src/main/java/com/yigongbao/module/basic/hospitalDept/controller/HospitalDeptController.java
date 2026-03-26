package com.yigongbao.module.basic.hospitalDept.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.module.basic.hospitalDept.dto.CreateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.dto.UpdateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医院科室 Controller
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Tag(name = "医院科室管理", description = "医院科室信息管理")
@RestController
@RequestMapping("/basic/hospital-dept")
@RequiredArgsConstructor
@Validated
public class HospitalDeptController {

    private final HospitalDeptService hospitalDeptService;

    /**
     * 分页查询科室列表
     */
    @Operation(summary = "分页查询科室列表")
    @GetMapping("/page")
    public Result<IPage<HospitalDeptVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String hospitalDeptName,
            @RequestParam(required = false) Integer status) {
        return Result.success(hospitalDeptService.listDepts(pageNum, pageSize, hospitalDeptName, status));
    }

    /**
     * 查询所有科室列表
     */
    @Operation(summary = "查询所有科室列表")
    @GetMapping("/list")
    public Result<List<HospitalDeptVO>> list(
            @RequestParam(required = false) String hospitalDeptName,
            @RequestParam(required = false) Integer status) {
        return Result.success(hospitalDeptService.listAll(hospitalDeptName, status));
    }

    /**
     * 根据ID查询科室
     */
    @Operation(summary = "根据ID查询科室")
    @GetMapping("/{id}")
    public Result<HospitalDeptVO> getById(@PathVariable Long id) {
        return Result.success(hospitalDeptService.getById(id));
    }

    /**
     * 创建科室
     */
    @Operation(summary = "创建科室")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建科室"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateHospitalDeptDTO dto) {
        hospitalDeptService.create(dto);
        return Result.success();
    }

    /**
     * 更新科室
     */
    @Operation(summary = "更新科室")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新科室"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateHospitalDeptDTO dto) {
        hospitalDeptService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除科室
     */
    @Operation(summary = "删除科室")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除科室"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        hospitalDeptService.remove(id);
        return Result.success();
    }

    /**
     * 修改状态
     */
    @Operation(summary = "修改科室状态")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改科室状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "状态不能为空") @Min(0) @Max(1) Integer status) {
        hospitalDeptService.updateStatus(id, status);
        return Result.success();
    }
}
