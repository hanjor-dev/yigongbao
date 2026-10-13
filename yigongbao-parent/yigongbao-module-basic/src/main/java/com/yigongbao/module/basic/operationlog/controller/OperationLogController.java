package com.yigongbao.module.basic.operationlog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequirePermission;
import com.yigongbao.framework.annotation.RequireSign;
import com.yigongbao.module.basic.operationlog.dto.OperationLogQueryDTO;
import com.yigongbao.module.basic.operationlog.service.OperationLogService;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 操作日志 Controller
 * 处理操作日志相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Tag(name = "操作日志管理", description = "系统操作日志查询和导出")
@RestController
@RequestMapping("/basic/operation-log")
@RequiredArgsConstructor
@RequireSign
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @Operation(summary = "分页查询操作日志")
    @PostMapping("/page")
    public Result<IPage<OperationLogVO>> page(@RequestBody OperationLogQueryDTO dto) {
        return Result.success(operationLogService.pageLogs(dto));
    }

    /**
     * 导出操作日志
     */
    @Operation(summary = "导出操作日志")
    @PostMapping("/export")
    public void export(@RequestBody OperationLogQueryDTO dto, HttpServletResponse response) {
        operationLogService.exportLogs(dto, response);
    }
}
