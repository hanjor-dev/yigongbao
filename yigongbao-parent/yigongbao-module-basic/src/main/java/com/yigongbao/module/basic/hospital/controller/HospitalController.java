package com.yigongbao.module.basic.hospital.controller;

/**
 * 医院管理 Controller
 * 处理医院相关的 HTTP 请求，包括 CRUD 和状态管理
 *
 * @author hanjor
 * @date 2026-03-19
 */

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.dto.UpdateHospitalDTO;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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
 * 医院 Controller
 * 处理医院相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-19
 */
@RestController
@RequestMapping("/api/basic/hospital")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * 分页查询医院列表
     */
    @GetMapping("/list")
    public Result<IPage<HospitalVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String hospitalName,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Integer hospitalLevel,
            @RequestParam(required = false) Integer hospitalType,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(hospitalService.listHospital(pageNum, pageSize, hospitalName, areaId, hospitalLevel, hospitalType, status));
    }

    /**
     * 根据ID查询医院详情
     */
    @GetMapping("/{id}")
    public Result<HospitalVO> getById(@PathVariable Long id) {
        return Result.success(hospitalService.getHospitalById(id));
    }

    /**
     * 创建医院
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateHospitalDTO dto) {
        hospitalService.createHospital(dto);
        return Result.success();
    }

    /**
     * 更新医院
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateHospitalDTO dto) {
        hospitalService.updateHospital(id, dto);
        return Result.success();
    }

    /**
     * 修改医院状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        hospitalService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 获取医院下拉选项
     */
    @GetMapping("/options")
    public Result<List<HospitalVO>> options(@RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(hospitalService.listOptions(status));
    }
}
