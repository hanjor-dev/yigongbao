package com.yigongbao.module.backup.identity.model;

import java.time.LocalDateTime;

/** 账号同步数据；passwordHash 仅作为内部传输字段，禁止写入日志。 */
public record AccountSyncRecord(Long id, String username, String passwordHash, Integer status,
                                String realName, String roleCode, LocalDateTime updateTime) {
}
