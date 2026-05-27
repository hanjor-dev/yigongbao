package com.yigongbao.module.production.process.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.system.config.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class ProcessConfigController {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;

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
            return Result.success(json);
        }
    }
}
