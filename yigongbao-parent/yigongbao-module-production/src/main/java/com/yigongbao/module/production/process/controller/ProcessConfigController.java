package com.yigongbao.module.production.process.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.process.vo.ProcessingCenterDevicesVO;
import com.yigongbao.module.production.record.vo.PrinterVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工序配置接口
 * 提供工序步骤定义和参数字段配置，供前端动态渲染流程导航和参数表单
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Tag(name = "工序配置")
@RestController
@RequestMapping("/production/process-config")
@RequiredArgsConstructor
@Slf4j
public class ProcessConfigController {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;

    private static final List<Map<String, Object>> PROCESS_STEPS = Arrays.stream(ProcessTypeEnum.values())
            .map(e -> {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("processType", e.getCode());
                step.put("processName", e.getDesc());
                step.put("processSeq", e.getOrder());
                return step;
            })
            .toList();

    @Operation(summary = "获取工序步骤定义")
    @GetMapping("/steps")
    public Result<List<Map<String, Object>>> getProcessSteps() {
        return Result.success(PROCESS_STEPS);
    }

    @Operation(summary = "获取工序参数配置字典")
    @GetMapping("/params")
    public Result<Object> getProcessParamsConfig() {
        String json = configService.getConfigValue(
                SystemConfigKeyEnum.PRODUCTION_PROCESS_PARAMS_CONFIG.getKey());
        try {
            return Result.success(objectMapper.readValue(json, new TypeReference<Object>() {}));
        } catch (Exception e) {
            log.warn("工序参数配置JSON解析失败: {}", e.getMessage());
            return Result.success(json);
        }
    }

    @Operation(summary = "按加工中心分组查询设备列表（生产员只查自己绑定的加工中心）")
    @GetMapping("/devices")
    public Result<List<ProcessingCenterDevicesVO>> listDevicesByType(
            @RequestParam String deviceType) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(userId);

        LambdaQueryWrapper<DeviceEntity> query = new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeviceType, deviceType)
                .eq(DeviceEntity::getIsDeleted, StatusConstants.NO);

        if (RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(currentUser.getRoleCode())) {
            if (currentUser.getCenterId() == null) {
                log.warn("生产员未绑定加工中心: userId={}", userId);
                return Result.success(Collections.emptyList());
            }
            query.eq(DeviceEntity::getCenterId, currentUser.getCenterId());
        }

        List<DeviceEntity> devices = deviceMapper.selectList(query);

        Map<Long, String> centerNameMap = devices.stream()
                .collect(Collectors.toMap(DeviceEntity::getCenterId, DeviceEntity::getCenterName, (v1, v2) -> v1));

        Map<Long, List<PrinterVO>> grouped = devices.stream()
                .collect(Collectors.groupingBy(
                        DeviceEntity::getCenterId,
                        Collectors.mapping(d -> {
                            PrinterVO vo = new PrinterVO();
                            vo.setId(d.getId());
                            vo.setDeviceNo(d.getDeviceId());
                            vo.setDeviceName(d.getDeviceName());
                            int s = d.getConnectionStatus() == null || d.getConnectionStatus() == 0 ? 0
                                    : Integer.valueOf(1).equals(d.getState()) ? 2 : 1;
                            vo.setStatus(s);
                            vo.setStatusName(s == 0 ? "离线" : s == 2 ? "繁忙" : "空闲");
                            return vo;
                        }, Collectors.toList())
                ));

        List<ProcessingCenterDevicesVO> result = grouped.entrySet().stream()
                .map(e -> {
                    ProcessingCenterDevicesVO vo = new ProcessingCenterDevicesVO();
                    vo.setCenterId(e.getKey());
                    vo.setCenterName(centerNameMap.get(e.getKey()));
                    vo.setDevices(e.getValue());
                    return vo;
                })
                .sorted(Comparator.comparing(ProcessingCenterDevicesVO::getCenterId))
                .collect(Collectors.toList());

        return Result.success(result);
    }
}
