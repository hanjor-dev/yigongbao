package com.yigongbao.module.backup.identity.model;

import java.util.List;

public record AccountSyncPage(List<AccountSyncRecord> items, AccountSyncCursor nextCursor,
                              boolean hasMore) {
    public AccountSyncPage {
        items = List.copyOf(items == null ? List.of() : items);
    }
}
