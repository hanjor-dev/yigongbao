package com.yigongbao.module.system.role.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.role.dto.CreateRoleDTO;
import com.yigongbao.module.system.role.dto.RolePageDTO;
import com.yigongbao.module.system.role.dto.UpdateRoleDTO;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.role.vo.RoleVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller
 * 处理角色相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Tag(name = "角色管理", description = "角色 CRUD、状态管理、分配资源")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
@RequireSign
public class RoleController {

    private final RoleService roleService;

    /**
     * 分页查询角色列表
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询角色列表")
    public Result<IPage<RoleVO>> list(@Validated @RequestBody RolePageDTO dto) {
        return Result.success(roleService.listRole(dto));
    }

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询角色详情")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    /**
     * 创建角色
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @Operation(summary = "创建角色")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建角色"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateRoleDTO dto) {
        roleService.createRole(dto);
        return Result.success();
    }

    /**
     * 更新角色
     *
     * @param id  角色ID
     * @param dto 更新参数
     * @return 更新结果
     */
    @Operation(summary = "更新角色")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新角色"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateRoleDTO dto) {
        roleService.updateRole(id, dto);
        return Result.success();
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 删除结果
     */
    @Operation(summary = "删除角色")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除角色"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        roleService.removeRole(id);
        return Result.success();
    }

    /**
     * 修改角色状态
     *
     * @param id     角色ID
     * @param status 状态值（0=禁用，1=正常）
     * @return 操作结果
     */
    @Operation(summary = "修改角色状态")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改角色状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        roleService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 全量查询角色列表（用于前端下拉选择）
     *
     * @return 角色列表（包含关联名称）
     */
    @GetMapping("/all")
    @Operation(summary = "全量查询角色列表（用于下拉选择）")
    public Result<List<RoleVO>> listAll() {
        return Result.success(roleService.listAllRole());
    }
}
