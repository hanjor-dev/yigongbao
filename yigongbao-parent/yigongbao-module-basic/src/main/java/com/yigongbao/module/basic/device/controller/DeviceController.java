package com.yigongbao.module.basic.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.common.vo.SelectTreeVO;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.dto.UpdateDeviceDTO;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备管理控制器
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Tag(name = "设备管理", description = "设备信息管理")
@RestController
@RequestMapping("/basic/device")
@RequiredArgsConstructor
@RequireSign
public class DeviceController {

    private final IDeviceService deviceService;

    /**
     * 分页查询设备列表
     */
    @Operation(summary = "分页查询设备列表")
    @PostMapping("/list")
    public Result<IPage<DeviceVO>> list(@Validated @RequestBody DevicePageDTO dto) {
        return Result.success(deviceService.listDevices(dto));
    }

    /**
     * 根据ID查询设备详情
     */
    @Operation(summary = "根据ID查询设备详情")
    @GetMapping("/{id}")
    public Result<DeviceVO> getById(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceById(id));
    }

    /**
     * 手动创建设备
     */
    @Operation(summary = "手动创建设备")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建设备"
    )
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateDeviceDTO dto) {
        return Result.success(deviceService.createDevice(dto));
    }

    /**
     * 手动更新设备状态
     */
    @Operation(summary = "手动更新设备状态")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新设备状态"
    )
    @PutMapping("/{id}/state")
    public Result<Void> updateState(
            @PathVariable Long id,
            @RequestParam @NotNull @Min(0) @Max(6) Integer state) {
        deviceService.updateDeviceState(id, state);
        return Result.success();
    }

    /**
     * 查询指定加工中心下某类型的所有设备（用于任务分配）
     */
    @Operation(summary = "查询中心设备列表")
    @GetMapping("/by-center")
    public Result<List<DeviceVO>> listByCenterAndType(@RequestParam(required = false) Long centerId,
                                                       @RequestParam(required = false) String deviceType) {
        return Result.success(deviceService.listDevicesByCenterAndType(centerId, deviceType));
    }

    /**
     * 查询设备类型下拉列表
     */
    @Operation(summary = "查询设备类型下拉列表")
    @GetMapping("/types")
    public Result<List<SelectTreeVO>> listDeviceTypes() {
        return Result.success(DEVICE_TYPE_OPTIONS);
    }

    /**
     * 编辑设备信息
     */
    @Operation(summary = "编辑设备信息")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "编辑设备信息"
    )
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UpdateDeviceDTO dto) {
        deviceService.updateDevice(dto);
        return Result.success();
    }

    /**
     * 删除设备
     */
    @Operation(summary = "删除设备")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除设备"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        deviceService.removeDevice(id);
        return Result.success();
    }

    private static final List<SelectTreeVO> DEVICE_TYPE_OPTIONS = Arrays.stream(DeviceTypeEnum.values())
            .map(e -> {
                SelectTreeVO vo = new SelectTreeVO();
                vo.setValue(e.getCode());
                vo.setName(e.getName());
                return vo;
            })
            .collect(Collectors.toUnmodifiableList());
}
