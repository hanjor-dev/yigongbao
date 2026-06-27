package com.yigongbao.module.system.org.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.OrgPageDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.org.vo.OrgVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import com.yigongbao.module.system.org.vo.OrgHospitalChangeCheckVO;
import com.yigongbao.module.system.org.vo.OrgOperationCheckVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机构管理 Controller
 * 处理机构相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Tag(name = "机构管理", description = "机构 CRUD、状态管理")
@RestController
@RequestMapping("/system/org")
@RequiredArgsConstructor
@RequireSign
public class OrgController {

    private final OrgService orgService;

    /**
     * 分页查询机构列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询机构列表")
    public Result<IPage<OrgVO>> list(@Validated @RequestBody OrgPageDTO dto) {
        return Result.success(orgService.listOrg(dto));
    }

    /**
     * 根据ID查询机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询机构详情")
    public Result<OrgVO> getById(@PathVariable Long id) {
        return Result.success(orgService.getOrgById(id));
    }

    /**
     * 创建机构
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @Operation(summary = "创建机构")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建机构"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateOrgDTO dto) {
        orgService.createOrg(dto);
        return Result.success();
    }

    /**
     * 更新机构
     *
     * @param id 机构ID
     * @param dto 更新参数
     * @return 更新结果
     */
    @Operation(summary = "更新机构")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新机构"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateOrgDTO dto) {
        orgService.updateOrg(id, dto);
        return Result.success();
    }

    /**
     * 删除机构
     *
     * @param id 机构ID
     * @return 删除结果
     */
    @Operation(summary = "删除机构")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除机构"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        orgService.removeOrg(id);
        return Result.success();
    }

    /**
     * 修改机构状态
     *
     * @param id     机构ID
     * @param status 状态值（0=禁用，1=正常）
     * @return 操作结果
     */
    @Operation(summary = "修改机构状态")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改机构状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        orgService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 全量查询机构列表（用于前端下拉选择）
     *
     * @return 机构列表（包含字典名称）
     */
    @GetMapping("/all")
    @Operation(summary = "全量查询机构列表（用于下拉选择）")
    public Result<List<OrgVO>> listAll() {
        return Result.success(orgService.listAllOrg());
    }

    /**
     * 预检查经销商关联医院变更对用户权限的影响
     * 更新前调用，若 affected=true 需提示用户确认后再提交更新
     *
     * @param id             经销商机构ID
     * @param newHospitalIds 新的关联医院ID列表
     * @return 检查结果
     */
    @PostMapping("/{id}/hospital-change-check")
    @Operation(summary = "预检查经销商关联医院变更影响")
    public Result<OrgHospitalChangeCheckVO> checkHospitalChange(
            @PathVariable Long id,
            @RequestBody List<Long> newHospitalIds) {
        return Result.success(orgService.checkHospitalChange(id, newHospitalIds));
    }

    /**
     * 预检查删除机构的影响
     */
    @GetMapping("/{id}/check-remove")
    @Operation(summary = "预检查删除机构影响")
    public Result<OrgOperationCheckVO> checkRemove(@PathVariable Long id) {
        return Result.success(orgService.checkRemove(id));
    }

    /**
     * 预检查禁用机构的影响
     */
    @GetMapping("/{id}/check-disable")
    @Operation(summary = "预检查禁用机构影响")
    public Result<OrgOperationCheckVO> checkDisable(@PathVariable Long id) {
        return Result.success(orgService.checkDisable(id));
    }

    /**
     * 导出机构列表为 Excel
     */
    @GetMapping("/export")
    @Operation(summary = "导出机构列表")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.EXPORT,
            operation = "导出机构列表"
    )
    public void export(HttpServletResponse response) {
        orgService.exportOrgs(response);
    }
}
