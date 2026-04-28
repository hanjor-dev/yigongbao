package com.yigongbao.module.system.user.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.system.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
import com.yigongbao.module.system.org.vo.OrgVO;
import com.yigongbao.module.system.user.dto.AssignHospitalsDTO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 用户-医院关联 Controller
 * 处理用户医院分配相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Tag(name = "用户医院关联管理", description = "用户与医院的多对多关联管理")
@RestController
@RequestMapping("/system/user/{userId}/hospitals")
@RequiredArgsConstructor
public class UserHospitalController {

    private final UserHospitalService userHospitalService;
    private final HospitalGroupTemplateService hospitalGroupTemplateService;

    /**
     * 查询用户的医院列表
     */
    @Operation(summary = "查询用户的医院列表")
    @GetMapping
    public Result<List<OrgVO>> getHospitals(@PathVariable Long userId) {
        return Result.success(userHospitalService.getHospitalsByUserId(userId));
    }

    /**
     * 分配用户医院范围（覆盖式）
     */
    @Operation(summary = "分配用户医院范围（覆盖式）")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.ASSIGN,
            operation = "分配用户医院"
    )
    @PutMapping
    public Result<Void> assignHospitals(@PathVariable Long userId, @Valid @RequestBody AssignHospitalsDTO dto) {
        userHospitalService.assignHospitals(userId, dto.getHospitalIds());
        return Result.success();
    }

    /**
     * 获取可分配给用户的医院列表（管理员分配时使用）
     */
    @Operation(summary = "获取可分配给用户的医院列表（管理员分配时使用）")
    @PostMapping("/options")
    public Result<List<OrgVO>> getHospitalOptions(@PathVariable Long userId) {
        return Result.success(userHospitalService.getHospitalOptionsByUserId(userId));
    }

    /**
     * 预览模板包含的医院列表
     * 管理员选择模板后，预览模板内预设的医院，可基于此列表微调后提交
     *
     * @param userId     用户ID（路径参数，保持路由一致性）
     * @param templateId 模板ID
     * @return 模板包含的医院列表
     */
    @Operation(summary = "预览模板包含的医院列表")
    @GetMapping("/template/{templateId}")
    public Result<HospitalGroupTemplateVO> previewTemplate(@PathVariable Long userId, @PathVariable Long templateId) {
        // 用户分配场景：传入 userId，assigned 表示该用户是否已分配该医院
        return Result.success(hospitalGroupTemplateService.getTemplateById(templateId, userId));
    }
}
