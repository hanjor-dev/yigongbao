package com.yigongbao.module.basic.rebuildProject.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.dto.UpdateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectOptionVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/basic/rebuild-project")
@RequiredArgsConstructor
public class RebuildProjectController {

    private final RebuildProjectService rebuildProjectService;

    /**
     * 获取项目树形结构
     */
    @GetMapping("/tree")
    public Result<List<RebuildProjectVO>> tree(@RequestParam(required = false) String category) {
        return Result.success(rebuildProjectService.listTree(category));
    }

    /**
     * 根据部位ID获取项目列表
     */
    @GetMapping("/by-body-part/{bodyPartId}")
    public Result<List<RebuildProjectVO>> byBodyPart(
            @PathVariable Long bodyPartId,
            @RequestParam(required = false) String category) {
        return Result.success(rebuildProjectService.listByBodyPartId(bodyPartId, category));
    }

    /**
     * 获取项目下拉选项
     */
    @GetMapping("/options")
    public Result<List<RebuildProjectOptionVO>> options(
            @RequestParam(required = false) Long bodyPartId,
            @RequestParam(required = false) String category) {
        return Result.success(rebuildProjectService.listOptions(bodyPartId, category));
    }

    /**
     * 查询项目详情
     */
    @GetMapping("/{id}")
    public Result<RebuildProjectDetailVO> getById(@PathVariable Long id) {
        return Result.success(rebuildProjectService.getDetailById(id));
    }

    /**
     * 创建项目
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateRebuildProjectDTO dto) {
        rebuildProjectService.createProject(dto);
        return Result.success();
    }

    /**
     * 更新项目
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateRebuildProjectDTO dto) {
        rebuildProjectService.updateProject(id, dto);
        return Result.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rebuildProjectService.removeProject(id);
        return Result.success();
    }

    /**
     * 修改项目状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @NotNull @Min(0) @Max(1) Integer status) {
        rebuildProjectService.updateStatus(id, status);
        return Result.success();
    }
}
