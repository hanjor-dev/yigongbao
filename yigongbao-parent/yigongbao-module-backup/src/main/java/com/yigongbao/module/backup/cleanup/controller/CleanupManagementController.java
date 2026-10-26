package com.yigongbao.module.backup.cleanup.controller;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.module.backup.cleanup.CleanupTaskService;
import com.yigongbao.common.result.Result;
import com.yigongbao.framework.annotation.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 线上 PC 管理端 COS 清理接口；仅允许系统管理员和公司管理员访问。 */
@RestController
@RequestMapping({"/api/backup/cleanup/v1", "/backup/cleanup"})
public class CleanupManagementController {
    private final CleanupTaskService service;
    public CleanupManagementController(CleanupTaskService service) { this.service = service; }
    private void admin() {
        Object role = StpUtil.getSession().get("roleCode");
        if (!"admin".equals(role) && !"company-admin".equals(role)) {
            throw new NotPermissionException("backup-cleanup");
        }
    }
    /** 管理菜单未授权时返回标准 403，避免被通用异常处理器转换为 500。 */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<?>> forbidden(NotPermissionException exception) {
        return ResponseEntity.ok(Result.error(403, "无权执行 COS 清理操作"));
    }
    @GetMapping("/tasks")
    @RequirePermission("backup-cleanup:List")
    public ResponseEntity<Result<?>> list() { admin(); return ResponseEntity.ok(Result.success(service.list())); }
    @PostMapping("/evaluate")
    @RequirePermission("backup-cleanup:DryRun")
    public ResponseEntity<Result<?>> evaluate(@RequestBody CleanupTaskController.Days body) { admin(); return ResponseEntity.ok(Result.success(service.evaluate(body.olderThanDays()))); }
    @GetMapping("/tasks/{id}")
    @RequirePermission("backup-cleanup:View")
    public ResponseEntity<Result<?>> get(@PathVariable String id) { admin(); return ResponseEntity.ok(Result.success(service.get(id))); }
    @PostMapping("/tasks/{id}/pause")
    @RequirePermission("backup-cleanup:Pause")
    public ResponseEntity<Result<?>> pause(@PathVariable String id) { admin(); return ResponseEntity.ok(Result.success(service.pause(id))); }
    @PostMapping("/tasks/{id}/resume")
    @RequirePermission("backup-cleanup:Resume")
    public ResponseEntity<Result<?>> resume(@PathVariable String id) { admin(); return ResponseEntity.ok(Result.success(service.resume(id))); }
}
