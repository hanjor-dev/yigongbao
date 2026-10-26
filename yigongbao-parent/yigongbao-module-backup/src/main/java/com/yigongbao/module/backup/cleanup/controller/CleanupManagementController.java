package com.yigongbao.module.backup.cleanup.controller;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.module.backup.cleanup.CleanupTaskService;
import com.yigongbao.module.backup.common.response.InternalApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 线上 PC 管理端 COS 清理接口；仅允许系统管理员和公司管理员访问。 */
@RestController
@RequestMapping("/backup/cleanup")
public class CleanupManagementController {
    private final CleanupTaskService service;
    public CleanupManagementController(CleanupTaskService service) { this.service = service; }
    private void admin() {
        if (!StpUtil.hasRole("admin") && !StpUtil.hasRole("company-admin")) {
            throw new NotPermissionException("backup-cleanup");
        }
    }
    private static String requestId() { return "backup-cleanup"; }
    @GetMapping("/tasks")
    public ResponseEntity<InternalApiResponse<?>> list() { admin(); return ResponseEntity.ok(InternalApiResponse.success(service.list(), requestId())); }
    @PostMapping("/evaluate")
    public ResponseEntity<InternalApiResponse<?>> evaluate(@RequestBody CleanupTaskController.Days body) { admin(); return ResponseEntity.ok(InternalApiResponse.success(service.evaluate(body.olderThanDays()), requestId())); }
    @PostMapping("/tasks")
    public ResponseEntity<InternalApiResponse<?>> create(@RequestBody CleanupTaskController.Create body) { admin(); return ResponseEntity.ok(InternalApiResponse.success(service.create(body.olderThanDays(), body.objectKeys()), requestId())); }
    @GetMapping("/tasks/{id}")
    public ResponseEntity<InternalApiResponse<?>> get(@PathVariable String id) { admin(); return ResponseEntity.ok(InternalApiResponse.success(service.get(id), requestId())); }
    @PostMapping("/tasks/{id}/pause")
    public ResponseEntity<InternalApiResponse<?>> pause(@PathVariable String id) { admin(); return ResponseEntity.ok(InternalApiResponse.success(service.pause(id), requestId())); }
    @PostMapping("/tasks/{id}/resume")
    public ResponseEntity<InternalApiResponse<?>> resume(@PathVariable String id) { admin(); return ResponseEntity.ok(InternalApiResponse.success(service.resume(id), requestId())); }
}
