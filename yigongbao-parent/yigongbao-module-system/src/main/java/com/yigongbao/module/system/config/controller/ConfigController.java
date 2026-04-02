package com.yigongbao.module.system.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.config.dto.ConfigPageDTO;
import com.yigongbao.module.system.config.dto.CreateConfigDTO;
import com.yigongbao.module.system.config.dto.UpdateConfigDTO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.config.vo.ConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置 Controller
 * 处理配置相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Tag(name = "系统配置管理", description = "系统配置项的 CRUD")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * 分页查询配置列表
     */
    @Operation(summary = "分页查询配置列表")
    @PostMapping("/list")
    public Result<IPage<ConfigVO>> list(@Validated @RequestBody ConfigPageDTO dto) {
        return Result.success(configService.pageConfig(dto));
    }

    /**
     * 根据ID查询配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    @Operation(summary = "根据ID查询配置详情")
    @GetMapping("/{id}")
    public Result<ConfigVO> getById(@PathVariable Long id) {
        return Result.success(configService.getConfigById(id));
    }

    /**
     * 创建配置
     *
     * @param dto 创建配置请求参数
     * @return 创建结果
     */
    @Operation(summary = "创建配置")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建系统配置"
    )
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateConfigDTO dto) {
        configService.createConfig(dto);
        return Result.success();
    }

    /**
     * 更新配置
     *
     * @param id 配置ID
     * @param dto 更新配置请求参数
     * @return 更新结果
     */
    @Operation(summary = "更新配置")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新系统配置"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateConfigDTO dto) {
        configService.updateConfig(id, dto);
        return Result.success();
    }

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 删除结果
     */
    @Operation(summary = "删除配置")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除系统配置"
    )
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.deleteConfig(id);
        return Result.success();
    }

    /**
     * 获取所有公开配置（无需登录）
     *
     * @return 公开配置列表
     */
    @Operation(summary = "获取所有公开配置（无需登录）")
    @GetMapping("/public")
    public Result<List<ConfigVO>> listPublic() {
        return Result.success(configService.listPublicConfig());
    }
}
