package com.yigongbao.module.system.dept.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.yigongbao.module.system.dept.dto.CreateDeptDTO;
import com.yigongbao.module.system.dept.dto.UpdateDeptDTO;
import com.yigongbao.module.system.dept.service.DeptService;
import com.yigongbao.module.system.dept.vo.DeptVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 部门管理 Controller
 * 处理部门相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Tag(name = "部门管理", description = "部门 CRUD、状态管理")
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    /**
     * 分页查询部门列表
     *
     * @param pageNum  页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param orgId   所属机构ID
     * @param deptName 部门名称（模糊查询）
     * @param status   状态
     * @return 分页后的部门列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询部门列表")
    public Result<IPage<DeptVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Integer status) {
        return Result.success(deptService.listDept(pageNum, pageSize, orgId, deptName, status));
    }

    /**
     * 根据ID查询部门详情
     *
     * @param id 部门ID
     * @return 部门详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询部门详情")
    public Result<DeptVO> getById(@PathVariable Long id) {
        return Result.success(deptService.getDeptById(id));
    }

    /**
     * 创建部门
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @Operation(summary = "创建部门")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建部门"
    )
    @PostMapping
    public Result<Void> create(@Validated @RequestBody CreateDeptDTO dto) {
        deptService.createDept(dto);
        return Result.success();
    }

    /**
     * 更新部门
     *
     * @param id 部门ID
     * @param dto 更新参数
     * @return 更新结果
     */
    @Operation(summary = "更新部门")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新部门"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody UpdateDeptDTO dto) {
        deptService.updateDept(id, dto);
        return Result.success();
    }

    /**
     * 删除部门
     *
     * @param id 部门ID
     * @return 删除结果
     */
    @Operation(summary = "删除部门")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除部门"
    )
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        deptService.removeDept(id);
        return Result.success();
    }

    /**
     * 修改部门状态
     *
     * @param id     部门ID
     * @param status 状态值（0=禁用，1=正常）
     * @return 操作结果
     */
    @Operation(summary = "修改部门状态")
    @OperationLog(
            module = "系统管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "修改部门状态"
    )
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        deptService.updateStatus(id, status);
        return Result.success();
    }
}
