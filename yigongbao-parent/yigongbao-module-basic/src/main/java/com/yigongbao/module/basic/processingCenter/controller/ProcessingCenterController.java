package com.yigongbao.module.basic.processingCenter.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.enums.OperationTypeEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.OperationLog;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 加工中心管理控制器
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Tag(name = "加工中心管理", description = "加工中心信息管理")
@RestController
@RequestMapping("/basic/processing-center")
@RequiredArgsConstructor
@RequireSign
public class ProcessingCenterController {

    private final IProcessingCenterService processingCenterService;

    /**
     * 分页查询加工中心列表
     */
    @Operation(summary = "分页查询加工中心列表")
    @PostMapping("/list")
    public Result<IPage<ProcessingCenterVO>> list(@RequestBody ProcessingCenterPageDTO dto) {
        return Result.success(processingCenterService.listProcessingCenters(dto));
    }

    /**
     * 根据ID查询加工中心详情
     */
    @Operation(summary = "根据ID查询加工中心详情")
    @GetMapping("/{id}")
    public Result<ProcessingCenterVO> getById(@PathVariable Long id) {
        return Result.success(processingCenterService.getProcessingCenterById(id));
    }

    /**
     * 创建加工中心
     */
    @Operation(summary = "创建加工中心")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.CREATE,
            operation = "创建加工中心"
    )
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateProcessingCenterDTO dto) {
        return Result.success(processingCenterService.createProcessingCenter(dto));
    }

    /**
     * 更新加工中心
     */
    @Operation(summary = "更新加工中心")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.UPDATE,
            operation = "更新加工中心"
    )
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateProcessingCenterDTO dto) {
        dto.setId(id);
        processingCenterService.updateProcessingCenter(dto);
        return Result.success();
    }

    /**
     * 删除加工中心
     */
    @Operation(summary = "删除加工中心")
    @OperationLog(
            module = "基础管理",
            businessType = OperationTypeEnum.DELETE,
            operation = "删除加工中心"
    )
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        processingCenterService.deleteProcessingCenter(id);
        return Result.success();
    }

    /**
     * 查询所有启用的加工中心（用于下拉选择）
     */
    @Operation(summary = "查询所有启用的加工中心")
    @GetMapping("/all")
    public Result<List<ProcessingCenterVO>> listAll() {
        return Result.success(processingCenterService.listAllEnabled());
    }
}
