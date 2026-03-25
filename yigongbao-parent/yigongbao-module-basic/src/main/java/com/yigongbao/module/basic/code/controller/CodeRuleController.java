package com.yigongbao.module.basic.code.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.code.dto.CreateCodeRuleDTO;
import com.yigongbao.module.basic.code.dto.UpdateCodeRuleDTO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.code.service.CodeRuleService;
import com.yigongbao.module.basic.code.vo.CodeRuleVO;
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
@RestController
@RequestMapping("/api/basic/code")
@RequiredArgsConstructor
public class CodeRuleController {

    private final CodeGeneratorService codeGeneratorService;
    private final CodeRuleService codeRuleService;

    /**
     * 生成编码
     */
    @GetMapping("/generate/{ruleCode}")
    public Result<Map<String, String>> generate(@PathVariable String ruleCode) {
        String code = codeGeneratorService.generate(ruleCode);
        return Result.success(Map.of("code", code));
    }

    /**
     * 预览编码格式
     */
    @GetMapping("/preview/{ruleCode}")
    public Result<Map<String, String>> preview(@PathVariable String ruleCode) {
        String preview = codeGeneratorService.preview(ruleCode);
        return Result.success(Map.of("preview", preview));
    }

    /**
     * 分页查询编码规则
     */
    @GetMapping("/rule/list")
    public Result<IPage<CodeRuleVO>> listRules(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) Integer status) {
        return Result.success(codeRuleService.listRules(pageNum, pageSize, ruleCode, ruleName, status));
    }

    /**
     * 根据规则编码查询
     */
    @GetMapping("/rule/{ruleCode}")
    public Result<CodeRuleVO> getByRuleCode(@PathVariable String ruleCode) {
        return Result.success(codeRuleService.getByRuleCode(ruleCode));
    }

    /**
     * 创建编码规则
     */
    @PostMapping("/rule")
    public Result<Void> create(@Validated @RequestBody CreateCodeRuleDTO dto) {
        codeRuleService.createRule(dto);
        return Result.success();
    }

    /**
     * 更新编码规则
     */
    @PutMapping("/rule/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateCodeRuleDTO dto) {
        codeRuleService.updateRule(id, dto);
        return Result.success();
    }

    /**
     * 删除编码规则
     */
    @DeleteMapping("/rule/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        codeRuleService.removeRule(id);
        return Result.success();
    }
}
