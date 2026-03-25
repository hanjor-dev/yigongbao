package com.yigongbao.module.basic.operationlog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.basic.operationlog.dto.OperationLogQueryDTO;
import com.yigongbao.module.basic.operationlog.service.OperationLogService;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 操作日志 Controller
 * 处理操作日志相关的 HTTP 请求
 *
 * @author hanjor
 * @date 2026-03-24
 */
@RestController
@RequestMapping("/api/basic/operation-log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    public Result<IPage<OperationLogVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        OperationLogQueryDTO dto = new OperationLogQueryDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        dto.setModule(module);
        dto.setBusinessType(businessType);
        dto.setOperation(operation);
        dto.setUsername(username);
        dto.setIp(ip);
        dto.setStatus(status);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        return Result.success(operationLogService.pageLogs(dto));
    }

    /**
     * 导出操作日志
     */
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response) {
        OperationLogQueryDTO dto = new OperationLogQueryDTO();
        dto.setModule(module);
        dto.setBusinessType(businessType);
        dto.setOperation(operation);
        dto.setUsername(username);
        dto.setIp(ip);
        dto.setStatus(status);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        operationLogService.exportLogs(dto, response);
    }
}
