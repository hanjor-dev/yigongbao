package com.yigongbao.module.system.org.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.org.vo.OrgVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 机构管理 Controller
 * 处理机构相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-16
 */
@RestController
@RequestMapping("/api/system/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /**
     * 分页查询机构列表
     *
     * @param pageNum  页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param orgName 机构名称（模糊查询）
     * @param orgType 机构类型
     * @param areaId  地区ID
     * @param status  状态
     * @return 分页后的机构列表
     */
    @GetMapping("/list")
    public Result<IPage<OrgVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) Integer orgType,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Integer status) {
        return Result.success(orgService.listOrg(pageNum, pageSize, orgName, orgType, areaId, status));
    }

    /**
     * 根据ID查询机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    @GetMapping("/{id}")
    public Result<OrgVO> getById(@PathVariable Long id) {
        return Result.success(orgService.getOrgById(id));
    }

    /**
     * 创建机构
     *
     * @param dto 创建参数
     * @return 创建结果
     */
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
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        orgService.updateStatus(id, status);
        return Result.success();
    }
}
