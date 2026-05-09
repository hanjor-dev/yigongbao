package com.yigongbao.module.basic.rebuildProject.controller;

import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.dto.RebuildProjectByBodyPartDTO;
import com.yigongbao.module.basic.rebuildProject.dto.RebuildProjectFullTreeDTO;
import com.yigongbao.module.basic.rebuildProject.dto.RebuildProjectTreeDTO;
import com.yigongbao.module.basic.rebuildProject.dto.UpdateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.basic.rebuildProject.vo.BodyPartProjectTreeVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 重建项目 Controller
 * 处理项目相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Tag(name = "重建项目管理", description = "3D 重建项目信息管理")
@RestController
@RequestMapping("/basic/rebuild-project")
@RequiredArgsConstructor
@RequireSign
public class RebuildProjectController {

    private final RebuildProjectService rebuildProjectService;

    /**
     * 获取项目树形结构
     */
    @Operation(summary = "获取项目树形结构")
    @PostMapping("/tree")
    public Result<List<RebuildProjectVO>> tree(@RequestBody RebuildProjectTreeDTO dto) {
        return Result.success(rebuildProjectService.listTree(dto.getCategoryCode(), dto.getKeyword()));
    }

    /**
     * 根据部位ID获取项目列表
     */
    @Operation(summary = "根据部位ID获取项目列表")
    @PostMapping("/by-body-part")
    public Result<List<RebuildProjectVO>> byBodyPart(@Validated @RequestBody RebuildProjectByBodyPartDTO dto) {
        return Result.success(rebuildProjectService.listByBodyPartId(dto.getBodyPartId(), dto.getCategoryCode(), dto.getKeyword()));
    }

    /**
     * 获取完整部位-项目树形结构
     * 前端选项场景使用，支持 bodyPartId/categoryCode/keyword 三个可选过滤参数
     */
    @Operation(summary = "获取完整部位-项目树形结构")
    @PostMapping("/full-tree")
    public Result<List<BodyPartProjectTreeVO>> fullTree(@RequestBody RebuildProjectFullTreeDTO dto) {
        return Result.success(rebuildProjectService.listFullTree(dto.getCategoryCode(), dto.getBodyPartId(), dto.getKeyword()));
    }

    /**
     * 查询项目详情
     */
    @Operation(summary = "查询项目详情")
    @GetMapping("/{id}")
    public Result<RebuildProjectDetailVO> getById(@PathVariable Long id) {
        return Result.success(rebuildProjectService.getDetailById(id));
    }

    /**
     * 创建项目
     */
    @Operation(summary = "创建项目")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建重建项目"
    )
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateRebuildProjectDTO dto) {
        rebuildProjectService.createProject(dto);
        return Result.success();
    }

    /**
     * 更新项目
     */
    @Operation(summary = "更新项目")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新重建项目"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateRebuildProjectDTO dto) {
        rebuildProjectService.updateProject(id, dto);
        return Result.success();
    }

    /**
     * 删除项目
     */
    @Operation(summary = "删除项目")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除重建项目"
    )
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rebuildProjectService.removeProject(id);
        return Result.success();
    }

    /**
     * 修改项目状态
     */
    @Operation(summary = "修改项目状态")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改重建项目状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull @Min(0) @Max(1) Integer status) {
        rebuildProjectService.updateStatus(id, status);
        return Result.success();
    }
}
