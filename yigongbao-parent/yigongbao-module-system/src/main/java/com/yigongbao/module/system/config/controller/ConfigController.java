package com.yigongbao.module.system.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.config.dto.CreateConfigDTO;
import com.yigongbao.module.system.config.dto.UpdateConfigDTO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.config.vo.ConfigVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置 Controller
 * 处理配置相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-18
 */
@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * 分页查询配置列表
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param configKey 配置键
     * @param configName 配置名称
     * @param configGroup 配置分组
     * @param configType 配置类型
     * @param status 状态
     * @return 分页后的配置列表
     */
    @GetMapping("/list")
    public Result<IPage<ConfigVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) String configGroup,
            @RequestParam(required = false) String configType,
            @RequestParam(required = false) Integer status) {
        IPage<ConfigVO> page = configService.pageConfig(pageNum, pageSize, configKey, configName, configGroup, configType, status);
        return Result.success(page);
    }

    /**
     * 根据ID查询配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
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
    @GetMapping("/public")
    public Result<List<ConfigVO>> listPublic() {
        return Result.success(configService.listPublicConfig());
    }
}
