package com.yigongbao.module.notification.announcement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.notification.announcement.dto.AnnouncementCreateDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementPageDTO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementPendingVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementPreviewVO;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementAuditLogEntity;
import com.yigongbao.module.notification.announcement.dto.AnnouncementRecipientPageDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementTargetDTO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementRecipientVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementStatisticsVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementDetailVO;

import java.util.List;

public interface ISystemAnnouncementService {
    IPage<SystemAnnouncementEntity> page(AnnouncementPageDTO query);

    AnnouncementDetailVO get(Long id);

    Long createDraft(AnnouncementCreateDTO dto, Long operatorId);

    void updateDraft(Long id, AnnouncementCreateDTO dto, Long operatorId);

    AnnouncementPreviewVO preview(AnnouncementCreateDTO dto);

    void publish(Long id, Long operatorId, String clientIp);

    void revoke(Long id, Long operatorId, String clientIp);

    List<AnnouncementPendingVO> listPending(Long userId);

    AnnouncementPendingVO getPending(Long announcementId, Long userId);

    int estimateTarget(AnnouncementTargetDTO target);

    void acknowledge(Long announcementId, Long userId);

    AnnouncementStatisticsVO statistics(Long id);

    IPage<AnnouncementRecipientVO> recipients(Long id, AnnouncementRecipientPageDTO query);

    IPage<SystemAnnouncementAuditLogEntity> auditLogs(Long id, Integer pageNum, Integer pageSize);
}
