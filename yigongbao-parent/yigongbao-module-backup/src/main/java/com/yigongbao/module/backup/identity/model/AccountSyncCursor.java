package com.yigongbao.module.backup.identity.model;

import java.time.LocalDateTime;

/** 账号增量同步游标，按 update_time、id 升序组成稳定排序。 */
public record AccountSyncCursor(LocalDateTime updateTime, Long id) {
}
