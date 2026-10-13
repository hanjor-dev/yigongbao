package com.yigongbao.module.notification.announcement.service;

import com.yigongbao.module.notification.announcement.dto.AnnouncementTargetDTO;
import com.yigongbao.module.notification.announcement.enums.AnnouncementTargetTypeEnum;
import com.yigongbao.module.notification.announcement.mapper.AnnouncementTargetMapper;
import com.yigongbao.module.notification.announcement.vo.AnnouncementTargetUserVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnnouncementTargetResolverTest {

    private final AnnouncementTargetMapper mapper = mock(AnnouncementTargetMapper.class);
    private final AnnouncementTargetResolver resolver = new AnnouncementTargetResolver(mapper);

    @Test
    void allTargetUsesActiveUsersIncludingAdmin() {
        AnnouncementTargetUserVO admin = user(1L, "admin");
        when(mapper.selectAllActiveUsers()).thenReturn(List.of(admin));

        AnnouncementTargetDTO target = new AnnouncementTargetDTO();
        target.setTargetType(AnnouncementTargetTypeEnum.ALL);

        assertEquals(List.of(admin), resolver.resolve(target));
    }

    @Test
    void duplicateUsersAreCollapsedBeforePublish() {
        AnnouncementTargetUserVO first = user(1L, "same");
        AnnouncementTargetUserVO duplicate = user(1L, "same");
        when(mapper.selectActiveUsersByRoleIds(List.of(2L, 3L))).thenReturn(List.of(first, duplicate));

        AnnouncementTargetDTO target = new AnnouncementTargetDTO();
        target.setTargetType(AnnouncementTargetTypeEnum.ROLE);
        target.setRoleIds(List.of(2L, 3L));

        assertEquals(1, resolver.resolve(target).size());
    }

    private AnnouncementTargetUserVO user(Long id, String username) {
        AnnouncementTargetUserVO user = new AnnouncementTargetUserVO();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }
}
