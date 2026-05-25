package com.yigongbao.module.basic.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.device.dto.CreateDeviceDTO;
import com.yigongbao.module.basic.device.dto.DevicePageDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.device.vo.DeviceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/basic/device")
@RequiredArgsConstructor
public class DeviceController {

    private final IDeviceService deviceService;

    @PostMapping("/list")
    public Result<IPage<DeviceVO>> list(@RequestBody DevicePageDTO dto) {
        return Result.success(deviceService.listDevices(dto));
    }

    @GetMapping("/{id}")
    public Result<DeviceVO> getById(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateDeviceDTO dto) {
        return Result.success(deviceService.createDevice(dto));
    }

    @PutMapping("/{id}/state")
    public Result<Void> updateState(@PathVariable Long id, @RequestParam Integer state) {
        deviceService.updateDeviceState(id, state);
        return Result.success();
    }

    @GetMapping("/idle")
    public Result<List<DeviceVO>> listIdle(@RequestParam(required = false) Long centerId,
                                            @RequestParam(required = false) String deviceType) {
        return Result.success(deviceService.listIdleDevices(centerId, deviceType));
    }
}
