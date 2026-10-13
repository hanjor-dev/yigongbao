package com.yigongbao.module.notification.announcement.service;

import com.yigongbao.module.notification.announcement.dto.AnnouncementCreateDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementAttachmentDTO;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementAttachmentEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementAuditLogEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementRecipientEntity;
import com.yigongbao.module.notification.announcement.enums.AnnouncementStatusEnum;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementAttachmentMapper;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementAuditLogMapper;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementMapper;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementRecipientMapper;
import com.yigongbao.module.notification.announcement.service.impl.SystemAnnouncementServiceImpl;
import com.yigongbao.module.notification.announcement.vo.AnnouncementTargetUserVO;
import com.yigongbao.module.notification.service.impl.NotificationPushService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemAnnouncementServiceImplTest {

    private final SystemAnnouncementMapper announcementMapper = mock(SystemAnnouncementMapper.class);
    private final SystemAnnouncementRecipientMapper recipientMapper = mock(SystemAnnouncementRecipientMapper.class);
    private final SystemAnnouncementAttachmentMapper attachmentMapper = mock(SystemAnnouncementAttachmentMapper.class);
    private final SystemAnnouncementAuditLogMapper auditLogMapper = mock(SystemAnnouncementAuditLogMapper.class);
    private final AnnouncementTargetResolver targetResolver = mock(AnnouncementTargetResolver.class);
    private final AnnouncementContentSanitizer sanitizer = new AnnouncementContentSanitizer();
    private final NotificationPushService pushService = mock(NotificationPushService.class);
    private final SystemAnnouncementServiceImpl service = new SystemAnnouncementServiceImpl(
            announcementMapper, recipientMapper, attachmentMapper, auditLogMapper,
            targetResolver, sanitizer, pushService);

    @Test
    void updatePublishedDraftIsRejected() {
        SystemAnnouncementEntity entity = new SystemAnnouncementEntity();
        entity.setId(1L);
        entity.setStatus(AnnouncementStatusEnum.PUBLISHED.getCode());
        when(announcementMapper.selectById(1L)).thenReturn(entity);

        assertThrows(RuntimeException.class, () -> service.updateDraft(1L, draft(), 9L));
        verify(announcementMapper, never()).updateById(any(SystemAnnouncementEntity.class));
    }

    @Test
    void acknowledgeOnlyCountsConditionalUpdateOnce() {
        when(recipientMapper.acknowledge(2L, 3L)).thenReturn(1);
        service.acknowledge(2L, 3L);
        verify(announcementMapper).update(any(), any());

        reset(announcementMapper, recipientMapper);
        when(recipientMapper.acknowledge(2L, 3L)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.acknowledge(2L, 3L));
        verify(announcementMapper, never()).update(any(), any());
    }

    @Test
    void publishUsesLockedRowAndBatchInsert() {
        SystemAnnouncementEntity entity = new SystemAnnouncementEntity();
        entity.setId(10L);
        entity.setStatus(AnnouncementStatusEnum.DRAFT.getCode());
        entity.setTargetType("ALL");
        entity.setTargetRoleIds("[]");
        entity.setTargetUserIds("[]");
        when(announcementMapper.selectForUpdate(10L)).thenReturn(entity);
        when(targetResolver.resolve(any())).thenReturn(List.of(user(7L)));

        service.publish(10L, 1L, "127.0.0.1");

        verify(announcementMapper).selectForUpdate(10L);
        verify(recipientMapper).insertBatch(any());
        verify(announcementMapper).updateById(entity);
        verify(auditLogMapper).insert(any(SystemAnnouncementAuditLogEntity.class));
    }

    @Test
    void previewRejectsExecutableAttachmentUrl() {
        AnnouncementCreateDTO dto = draft();
        AnnouncementAttachmentDTO attachment = new AnnouncementAttachmentDTO();
        attachment.setFileName("恶意链接");
        attachment.setFileUrl("javascript:alert(1)");
        dto.setAttachments(List.of(attachment));

        assertThrows(RuntimeException.class, () -> service.preview(dto));
    }

    private AnnouncementCreateDTO draft() {
        AnnouncementCreateDTO dto = new AnnouncementCreateDTO();
        dto.setTitle("公告");
        dto.setContentHtml("<p>内容</p>");
        return dto;
    }

    private AnnouncementTargetUserVO user(Long id) {
        AnnouncementTargetUserVO user = new AnnouncementTargetUserVO();
        user.setUserId(id);
        user.setUsername("user" + id);
        user.setRealName("用户" + id);
        return user;
    }
}
