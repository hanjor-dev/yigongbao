package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设计工单列配置 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "设计列配置", description = "设计工单列表列显示配置")
@RestController
@RequestMapping("/design/column-config")
@RequiredArgsConstructor
@RequireSign
public class DesignColumnConfigController {

    private final DesignWorkorderService designWorkorderService;

    /**
     * 获取当前用户的列配置
     */
    @Operation(summary = "获取列配置")
    @GetMapping
    public Result<DesignColumnConfigVO> getColumnConfig() {
        return Result.success(designWorkorderService.getColumnConfig());
    }

    /**
     * 保存当前用户的列配置
     */
    @Operation(summary = "保存列配置")
    @PostMapping
    public Result<Void> saveColumnConfig(@Validated @RequestBody SaveDesignColumnConfigDTO dto) {
        designWorkorderService.saveColumnConfig(dto);
        return Result.success();
    }
}
