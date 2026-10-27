package com.yigongbao.module.backup.restore.controller;

import com.yigongbao.module.backup.common.response.InternalApiResponse; import com.yigongbao.module.backup.restore.RestoreSessionService; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/internal/restore/v1")
public class RestoreController {private final RestoreSessionService service; public RestoreController(RestoreSessionService service){this.service=service;}
 @PostMapping("/orders/{orderId}/prepare") public ResponseEntity<InternalApiResponse<RestoreSessionService.RestoreSession>> prepare(@PathVariable Long orderId,@RequestBody Prepare c){return ResponseEntity.ok(InternalApiResponse.success(service.prepare(orderId,c.manifestRevision(),c.relationDigest()),"restore-prepare"));}
 @GetMapping("/sessions/{id}") public ResponseEntity<InternalApiResponse<RestoreSessionService.RestoreSession>> get(@PathVariable String id){return ResponseEntity.ok(InternalApiResponse.success(service.get(id),"restore-get"));}
 @PostMapping("/sessions/{id}/commit") public ResponseEntity<InternalApiResponse<RestoreSessionService.RestoreSession>> commit(@PathVariable String id,@RequestBody Commit c){return ResponseEntity.ok(InternalApiResponse.success(service.commit(id,c.uploaded()),"restore-commit"));}
 @PostMapping("/sessions/{id}/abort") public ResponseEntity<InternalApiResponse<RestoreSessionService.RestoreSession>> abort(@PathVariable String id,@RequestBody Abort c){return ResponseEntity.ok(InternalApiResponse.success(service.abort(id,c.reason()),"restore-abort"));}
 public record Prepare(String manifestRevision,String relationDigest){} public record Commit(List<RestoreSessionService.UploadedAsset> uploaded){} public record Abort(String reason){}
}
