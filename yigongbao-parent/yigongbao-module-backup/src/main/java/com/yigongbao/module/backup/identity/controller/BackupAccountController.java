package com.yigongbao.module.backup.identity.controller;

import com.yigongbao.module.backup.common.response.InternalApiResponse;
import com.yigongbao.module.backup.identity.AccountSyncService;
import com.yigongbao.module.backup.identity.model.AccountSyncCursor;
import com.yigongbao.module.backup.identity.model.AccountSyncPage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 线上账号增量同步内部接口；密码哈希只出现在受 HMAC 保护的响应体。 */
@RestController
@RequestMapping("/internal/backup/v1/accounts")
public class BackupAccountController {
    private final AccountSyncService accountSyncService;

    public BackupAccountController(AccountSyncService accountSyncService) {
        this.accountSyncService = accountSyncService;
    }

    @GetMapping
    public ResponseEntity<InternalApiResponse<AccountSyncPage>> list(
            @RequestParam(required = false) OffsetDateTime updateTime,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "100") int pageSize) {
        AccountSyncPage page = accountSyncService.listIncremental(
                updateTime == null || id == null ? null
                        : new AccountSyncCursor(updateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime(), id), pageSize);
        return ResponseEntity.ok(InternalApiResponse.success(page, "backup-account"));
    }
}
