package com.yigongbao.module.backup.cleanup.controller;
import com.yigongbao.module.backup.cleanup.CleanupTaskService; import com.yigongbao.module.backup.common.response.InternalApiResponse; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/internal/cleanup/v1") public class CleanupTaskController {private final CleanupTaskService service;public CleanupTaskController(CleanupTaskService s){service=s;}private static String rid(){return "cleanup";}
 @PostMapping("/evaluate") public ResponseEntity<InternalApiResponse<?>> evaluate(@RequestBody Days d){return ResponseEntity.ok(InternalApiResponse.success(service.evaluate(d.olderThanDays()),rid()));}
 @PostMapping("/tasks") public ResponseEntity<InternalApiResponse<?>> create(@RequestBody Create c){var task=c.idempotencyKey()==null?service.create(c.olderThanDays(),c.objectKeys()):service.create(c.olderThanDays(),c.objectKeys(),c.idempotencyKey());return ResponseEntity.ok(InternalApiResponse.success(task,rid()));}
 @GetMapping("/tasks/{id}") public ResponseEntity<InternalApiResponse<?>> get(@PathVariable String id){return ResponseEntity.ok(InternalApiResponse.success(service.get(id),rid()));}
 @GetMapping("/tasks/{id}/items") public ResponseEntity<InternalApiResponse<?>> items(@PathVariable String id){return ResponseEntity.ok(InternalApiResponse.success(service.items(id),rid()));}
 @PostMapping("/tasks/{id}/pause") public ResponseEntity<InternalApiResponse<?>> pause(@PathVariable String id){return ResponseEntity.ok(InternalApiResponse.success(service.pause(id),rid()));}
 @PostMapping("/tasks/{id}/resume") public ResponseEntity<InternalApiResponse<?>> resume(@PathVariable String id){return ResponseEntity.ok(InternalApiResponse.success(service.resume(id),rid()));}
 @PostMapping("/tasks/{id}/permits") public ResponseEntity<InternalApiResponse<?>> permit(@PathVariable String id){return ResponseEntity.ok(InternalApiResponse.success(service.issuePermit(id),rid()));}
 @PostMapping("/tasks/permits/{permitId}/revoke") public ResponseEntity<InternalApiResponse<?>> revoke(@PathVariable String permitId){return ResponseEntity.ok(InternalApiResponse.success(service.revoke(permitId),rid()));}
 public record Days(int olderThanDays){} public record Create(int olderThanDays,List<String> objectKeys,String idempotencyKey){public Create(int olderThanDays,List<String> objectKeys){this(olderThanDays,objectKeys,null);}}
}
