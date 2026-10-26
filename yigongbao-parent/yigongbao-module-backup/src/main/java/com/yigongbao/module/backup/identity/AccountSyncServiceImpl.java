package com.yigongbao.module.backup.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.backup.identity.model.AccountSyncCursor;
import com.yigongbao.module.backup.identity.model.AccountSyncPage;
import com.yigongbao.module.backup.identity.model.AccountSyncRecord;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 线上账号增量同步；仅返回本地认证所需字段，不输出密码哈希日志。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountSyncServiceImpl implements AccountSyncService {
    private final UserMapper userMapper;

    @Override
    public AccountSyncPage listIncremental(AccountSyncCursor cursor, int pageSize) {
        int safeSize = Math.max(1, Math.min(pageSize, 500));
        LambdaQueryWrapper<UserEntity> query = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getIsDeleted, 0)
                .and(w -> {
                    if (cursor == null || cursor.updateTime() == null || cursor.id() == null) {
                        return;
                    }
                    w.gt(UserEntity::getUpdateTime, cursor.updateTime())
                            .or(x -> x.eq(UserEntity::getUpdateTime, cursor.updateTime())
                                    .gt(UserEntity::getId, cursor.id()));
                })
                .orderByAsc(UserEntity::getUpdateTime)
                .orderByAsc(UserEntity::getId)
                .last("LIMIT " + (safeSize + 1));
        List<UserEntity> rows = userMapper.selectList(query);
        boolean hasMore = rows.size() > safeSize;
        List<UserEntity> pageRows = hasMore ? rows.subList(0, safeSize) : rows;
        List<AccountSyncRecord> records = pageRows.stream().map(this::toRecord).toList();
        AccountSyncCursor next = records.isEmpty() ? cursor : new AccountSyncCursor(
                records.get(records.size() - 1).updateTime(), records.get(records.size() - 1).id());
        log.info("备份账号增量同步完成: count={}, hasMore={}", records.size(), hasMore);
        return new AccountSyncPage(records, next, hasMore);
    }

    private AccountSyncRecord toRecord(UserEntity user) {
        LocalDateTime updateTime = user.getUpdateTime() == null ? user.getCreateTime() : user.getUpdateTime();
        return new AccountSyncRecord(user.getId(), user.getUsername(), user.getPassword(), user.getStatus(),
                user.getRealName(), user.getRoleCode(), updateTime);
    }
}
