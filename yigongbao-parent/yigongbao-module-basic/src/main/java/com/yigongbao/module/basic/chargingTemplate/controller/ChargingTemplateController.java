package com.yigongbao.module.basic.chargingTemplate.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.chargingTemplate.dto.CreateChargingTemplateDTO;
import com.yigongbao.module.basic.chargingTemplate.dto.UpdateChargingTemplateDTO;
import com.yigongbao.module.basic.chargingTemplate.service.ChargingTemplateService;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateDetailVO;
import com.yigongbao.module.basic.chargingTemplate.vo.ChargingTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 收费模板 Controller
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Tag(name = "收费模板管理", description = "收费模板的增删改查接口")
@RestController
@RequestMapping("/api/basic/charging-template")
@RequiredArgsConstructor
public class ChargingTemplateController {

    private final ChargingTemplateService chargingTemplateService;

    /**
     * 分页查询收费模板列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param templateName 模板名称
     * @return 分页结果
     */
    @Operation(summary = "分页查询收费模板列表")
    @GetMapping("/list")
    public Result<IPage<ChargingTemplateVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName) {
        IPage<ChargingTemplateVO> page = chargingTemplateService.listPage(pageNum, pageSize, templateName);
        return Result.success(page);
    }

    /**
     * 根据ID查询模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @Operation(summary = "根据ID查询模板详情")
    @GetMapping("/{id}")
    public Result<ChargingTemplateDetailVO> getById(@PathVariable Long id) {
        return Result.success(chargingTemplateService.getDetailById(id));
    }

    /**
     * 创建收费模板
     *
     * @param dto 创建参数
     * @return 模板ID
     */
    @Operation(summary = "创建收费模板")
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CreateChargingTemplateDTO dto) {
        Long id = chargingTemplateService.create(dto);
        return Result.success(id);
    }

    /**
     * 更新收费模板
     *
     * @param id 模板ID
     * @param dto 更新参数
     * @return 操作结果
     */
    @Operation(summary = "更新收费模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Validated @RequestBody UpdateChargingTemplateDTO dto) {
        chargingTemplateService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除收费模板
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @Operation(summary = "删除收费模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        chargingTemplateService.remove(id);
        return Result.success();
    }
}
