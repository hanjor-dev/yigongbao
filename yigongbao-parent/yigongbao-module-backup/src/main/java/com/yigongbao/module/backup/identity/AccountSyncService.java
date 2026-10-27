package com.yigongbao.module.backup.identity;

import com.yigongbao.module.backup.identity.model.AccountSyncCursor;
import com.yigongbao.module.backup.identity.model.AccountSyncPage;

public interface AccountSyncService {
    AccountSyncPage listIncremental(AccountSyncCursor cursor, int pageSize);
}
