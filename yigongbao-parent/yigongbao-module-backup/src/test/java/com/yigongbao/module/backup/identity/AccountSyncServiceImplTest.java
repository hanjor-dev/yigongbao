package com.yigongbao.module.backup.identity;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yigongbao.module.backup.identity.model.AccountSyncPage;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountSyncServiceImplTest {
    @Test
    void returnsStableIncrementalCursorAndDoesNotLogPassword() {
        UserMapper mapper = mock(UserMapper.class);
        UserEntity first = user(1L, LocalDateTime.of(2026, 1, 1, 0, 0));
        UserEntity second = user(2L, LocalDateTime.of(2026, 1, 2, 0, 0));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(first, second));

        AccountSyncPage page = new AccountSyncServiceImpl(mapper)
                .listIncremental(null, 1);

        assertThat(page.items()).hasSize(1);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor().id()).isEqualTo(1L);
        assertThat(page.items().get(0).passwordHash()).isEqualTo("$2a$10$stored-hash");
    }

    private static UserEntity user(Long id, LocalDateTime updateTime) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("u" + id);
        user.setPassword("$2a$10$stored-hash");
        user.setStatus(1);
        user.setUpdateTime(updateTime);
        user.setCreateTime(updateTime);
        return user;
    }
}
