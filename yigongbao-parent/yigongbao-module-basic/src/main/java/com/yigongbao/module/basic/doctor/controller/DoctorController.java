package com.yigongbao.module.basic.doctor.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.basic.doctor.service.DoctorService;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;
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
@RestController
@RequestMapping("/api/basic/doctor")
@RequiredArgsConstructor
@Validated
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * 分页查询医生列表
     */
    @GetMapping("/page")
    public Result<IPage<DoctorVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long hospitalDeptId,
            @RequestParam(required = false) Integer status) {
        return Result.success(doctorService.listDoctors(pageNum, pageSize, doctorName, hospitalId, hospitalDeptId, status));
    }

    /**
     * 查询所有医生列表
     */
    @GetMapping("/list")
    public Result<List<DoctorVO>> list(
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Integer status) {
        return Result.success(doctorService.listAll(doctorName, hospitalId, status));
    }

    /**
     * 根据ID查询医生
     */
    @GetMapping("/{id}")
    public Result<DoctorVO> getById(@PathVariable Long id) {
        return Result.success(doctorService.getById(id));
    }

    /**
     * 创建医生
     */
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateDoctorDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long creatorId) {
        doctorService.create(dto, creatorId);
        return Result.success();
    }

    /**
     * 更新医生
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateDoctorDTO dto) {
        doctorService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除医生
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        doctorService.remove(id);
        return Result.success();
    }

    /**
     * 修改状态
     */
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
    @GetMapping("/suggest")
    public Result<List<DoctorVO>> suggest(
            @RequestParam Long creatorId,
            @RequestParam Long hospitalId,
            @RequestParam(required = false) String keyword) {
        return Result.success(doctorService.listByCreatorAndHospital(creatorId, hospitalId, keyword));
    }

    /**
     * 快速添加医生（订单创建时调用）
     */
    @PostMapping("/quick-add")
    public Result<DoctorVO> quickAdd(@Validated @RequestBody QuickAddDoctorDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long creatorId) {
        return Result.success(doctorService.quickAdd(dto, creatorId));
    }
}
