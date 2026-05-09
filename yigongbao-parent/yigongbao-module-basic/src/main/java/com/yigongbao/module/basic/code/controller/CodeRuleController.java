package com.yigongbao.module.basic.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.code.dto.CodeRulePageDTO;
import com.yigongbao.module.basic.code.dto.CreateCodeRuleDTO;
import com.yigongbao.module.basic.code.dto.UpdateCodeRuleDTO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.code.service.CodeRuleService;
import com.yigongbao.module.basic.code.vo.CodeRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 编码规则 Controller
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Tag(name = "编码规则管理", description = "系统编码规则配置和管理")
@RestController
@RequestMapping("/basic/code")
@RequiredArgsConstructor
@RequireSign
public class CodeRuleController {

    private final CodeGeneratorService codeGeneratorService;
    private final CodeRuleService codeRuleService;

    /**
     * 生成编码
     */
    @Operation(summary = "生成编码")
    @GetMapping("/generate/{ruleCode}")
    public Result<Map<String, String>> generate(@PathVariable String ruleCode) {
        String code = codeGeneratorService.generate(ruleCode);
        return Result.success(Map.of("code", code));
    }

    /**
     * 预览编码格式
     */
    @Operation(summary = "预览编码格式")
    @GetMapping("/preview/{ruleCode}")
    public Result<Map<String, String>> preview(@PathVariable String ruleCode) {
        String preview = codeGeneratorService.preview(ruleCode);
        return Result.success(Map.of("preview", preview));
    }

    /**
     * 分页查询编码规则
     */
    @Operation(summary = "分页查询编码规则")
    @PostMapping("/rule/list")
    public Result<IPage<CodeRuleVO>> listRules(@Validated @RequestBody CodeRulePageDTO dto) {
        return Result.success(codeRuleService.listRules(dto));
    }

    /**
     * 根据规则编码查询
     */
    @Operation(summary = "根据规则编码查询")
    @GetMapping("/rule/{ruleCode}")
    public Result<CodeRuleVO> getByRuleCode(@PathVariable String ruleCode) {
        return Result.success(codeRuleService.getByRuleCode(ruleCode));
    }

    /**
     * 创建编码规则
     */
    @Operation(summary = "创建编码规则")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建编码规则"
    )
    @PostMapping("/rule")
    public Result<Void> create(@Validated @RequestBody CreateCodeRuleDTO dto) {
        codeRuleService.createRule(dto);
        return Result.success();
    }

    /**
     * 更新编码规则
     */
    @Operation(summary = "更新编码规则")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新编码规则"
    )
    @PutMapping("/rule/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateCodeRuleDTO dto) {
        codeRuleService.updateRule(id, dto);
        return Result.success();
    }

    /**
     * 删除编码规则
     */
    @Operation(summary = "删除编码规则")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除编码规则"
    )
    @DeleteMapping("/rule/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        codeRuleService.removeRule(id);
        return Result.success();
    }
}
