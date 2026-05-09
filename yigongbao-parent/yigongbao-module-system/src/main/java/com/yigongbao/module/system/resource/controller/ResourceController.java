package com.yigongbao.module.system.resource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.resource.dto.CreateResourceDTO;
import com.yigongbao.module.system.resource.dto.ResourcePageDTO;
import com.yigongbao.module.system.resource.dto.UpdateResourceDTO;
import com.yigongbao.module.system.resource.service.ResourceService;
import com.yigongbao.module.system.resource.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源管理 Controller
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Tag(name = "菜单资源管理", description = "系统菜单/资源管理、角色资源分配")
@RestController
@RequestMapping("/system/resource")
@RequiredArgsConstructor
@RequireSign
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * 获取资源树（管理后台）
     */
    @Operation(summary = "获取资源树（管理后台）")
    @GetMapping("/tree")
    public Result<List<ResourceVO>> getResourceTree() {
        return Result.success(resourceService.getResourceTree());
    }

    /**
     * 获取资源列表（分页）
     */
    @Operation(summary = "获取资源列表（分页）")
    @PostMapping("/list")
    public Result<IPage<ResourceVO>> list(@Validated @RequestBody ResourcePageDTO dto) {
        return Result.success(resourceService.pageResources(dto));
    }

    /**
     * 根据ID获取资源详情
     */
    @Operation(summary = "根据ID获取资源详情")
    @GetMapping("/{id}")
    public Result<ResourceVO> getById(@PathVariable Long id) {
        return Result.success(resourceService.getResourceById(id));
    }

    /**
     * 新增资源
     */
    @Operation(summary = "新增资源")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "新增菜单资源"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateResourceDTO dto) {
        resourceService.createResource(dto);
        return Result.success();
    }

    /**
     * 更新资源
     */
    @Operation(summary = "更新资源")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新菜单资源"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateResourceDTO dto) {
        resourceService.updateResource(id, dto);
        return Result.success();
    }

    /**
     * 删除资源
     */
    @Operation(summary = "删除资源")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除菜单资源"
    )
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return Result.success();
    }

    /**
     * 获取角色已分配的资源ID列表
     */
    @Operation(summary = "获取角色已分配的资源ID列表")
    @GetMapping("/role/{roleId}")
    public Result<List<Long>> getRoleResources(@PathVariable Long roleId) {
        return Result.success(resourceService.getResourceIdsByRoleId(roleId));
    }

    /**
     * 分配角色资源
     */
    @Operation(summary = "分配角色资源")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.ASSIGN,
            operation = "分配角色资源"
    )
    @PutMapping("/role/{roleId}")
    public Result<Void> assignRoleResources(@PathVariable Long roleId, @RequestBody List<Long> resourceIds) {
        resourceService.assignResources(roleId, resourceIds);
        return Result.success();
    }

    /**
     * 获取带分配状态的资源树（用于角色分配资源场景）
     * GET /tree/role/{roleId}  - 获取指定角色的资源树（含checked）
     * GET /tree/role/null      - 获取全部资源树（全部checked=false）
     *
     * @param roleId 角色ID
     * @return 资源树，含 checked 字段
     */
    @Operation(summary = "获取带分配状态的资源树（用于角色分配资源场景）")
    @GetMapping("/tree/role/{roleId}")
    public Result<List<ResourceVO>> getResourceTreeWithChecked(@PathVariable Long roleId) {
        return Result.success(resourceService.getResourceTreeWithChecked(roleId));
    }

    /**
     * 获取全部资源树（checked=false，用于新建角色时）
     *
     * @return 资源树，含 checked 字段（全部为false）
     */
    @Operation(summary = "获取全部资源树（checked=false，用于新建角色时）")
    @GetMapping("/tree/all")
    public Result<List<ResourceVO>> getAllResourceTree() {
        return Result.success(resourceService.getResourceTreeWithChecked(null));
    }
}
