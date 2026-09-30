package com.yigongbao.module.notification.announcement.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.notification.announcement.dto.AnnouncementAttachmentDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementCreateDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementPageDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementTargetDTO;
import com.yigongbao.module.notification.announcement.dto.AnnouncementRecipientPageDTO;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementAttachmentEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementAuditLogEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementEntity;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementRecipientEntity;
import com.yigongbao.module.notification.announcement.enums.AnnouncementAuditOperationEnum;
import com.yigongbao.module.notification.announcement.enums.AnnouncementRecipientStatusEnum;
import com.yigongbao.module.notification.announcement.enums.AnnouncementStatusEnum;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementAttachmentMapper;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementAuditLogMapper;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementMapper;
import com.yigongbao.module.notification.announcement.mapper.SystemAnnouncementRecipientMapper;
import com.yigongbao.module.notification.announcement.service.AnnouncementContentSanitizer;
import com.yigongbao.module.notification.announcement.service.AnnouncementTargetResolver;
import com.yigongbao.module.notification.announcement.service.ISystemAnnouncementService;
import com.yigongbao.module.notification.announcement.vo.AnnouncementPendingVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementPreviewVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementTargetUserVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementAttachmentVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementRecipientVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementStatisticsVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementDetailVO;
import com.yigongbao.module.notification.service.impl.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemAnnouncementServiceImpl implements ISystemAnnouncementService {

    private final SystemAnnouncementMapper announcementMapper;
    private final SystemAnnouncementRecipientMapper recipientMapper;
    private final SystemAnnouncementAttachmentMapper attachmentMapper;
    private final SystemAnnouncementAuditLogMapper auditLogMapper;
    private final AnnouncementTargetResolver targetResolver;
    private final AnnouncementContentSanitizer contentSanitizer;
    private final NotificationPushService notificationPushService;

    @Override
    public IPage<SystemAnnouncementEntity> page(AnnouncementPageDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<SystemAnnouncementEntity> wrapper = new LambdaQueryWrapper<SystemAnnouncementEntity>()
                .like(StringUtils.hasText(query.getTitle()), SystemAnnouncementEntity::getTitle, query.getTitle())
                .eq(StringUtils.hasText(query.getStatus()), SystemAnnouncementEntity::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getTargetType()), SystemAnnouncementEntity::getTargetType, query.getTargetType())
                .orderByDesc(SystemAnnouncementEntity::getPublishedAt)
                .orderByDesc(SystemAnnouncementEntity::getId);
        return announcementMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public AnnouncementDetailVO get(Long id) {
        SystemAnnouncementEntity entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return toDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(AnnouncementCreateDTO dto, Long operatorId) {
        validateAttachmentUrls(dto.getAttachments());
        String html = contentSanitizer.sanitize(dto.getContentHtml());
        SystemAnnouncementEntity entity = new SystemAnnouncementEntity();
        entity.setTitle(dto.getTitle().trim());
        entity.setContentHtml(html);
        entity.setContentText(contentSanitizer.toPlainText(html));
        entity.setStatus(AnnouncementStatusEnum.DRAFT.getCode());
        entity.setTargetType(dto.getTarget().getTargetType().getCode());
        entity.setTargetRoleIds(JSONUtil.toJsonStr(dto.getTarget().getRoleIds()));
        entity.setTargetUserIds(JSONUtil.toJsonStr(dto.getTarget().getUserIds()));
        entity.setForceConfirm(1);
        entity.setTargetCount(0);
        entity.setAcknowledgedCount(0);
        entity.setRevokedCount(0);
        entity.setCreateBy(operatorId);
        entity.setUpdateBy(operatorId);
        announcementMapper.insert(entity);
        replaceAttachments(entity.getId(), dto.getAttachments(), operatorId);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(Long id, AnnouncementCreateDTO dto, Long operatorId) {
        validateAttachmentUrls(dto.getAttachments());
        SystemAnnouncementEntity entity = announcementMapper.selectForUpdate(id);
        if (entity == null) throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        ensureStatus(entity, AnnouncementStatusEnum.DRAFT);
        String html = contentSanitizer.sanitize(dto.getContentHtml());
        entity.setTitle(dto.getTitle().trim());
        entity.setContentHtml(html);
        entity.setContentText(contentSanitizer.toPlainText(html));
        entity.setTargetType(dto.getTarget().getTargetType().getCode());
        entity.setTargetRoleIds(JSONUtil.toJsonStr(dto.getTarget().getRoleIds()));
        entity.setTargetUserIds(JSONUtil.toJsonStr(dto.getTarget().getUserIds()));
        entity.setUpdateBy(operatorId);
        announcementMapper.updateById(entity);
        replaceAttachments(id, dto.getAttachments(), operatorId);
    }

    @Override
    public AnnouncementPreviewVO preview(AnnouncementCreateDTO dto) {
        validateAttachmentUrls(dto.getAttachments());
        String html = contentSanitizer.sanitize(dto.getContentHtml());
        return new AnnouncementPreviewVO(dto.getTitle().trim(), html, contentSanitizer.toPlainText(html));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, Long operatorId, String clientIp) {
        SystemAnnouncementEntity entity = announcementMapper.selectForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        ensureStatus(entity, AnnouncementStatusEnum.DRAFT);
        AnnouncementTargetDTO target = new AnnouncementTargetDTO();
        target.setTargetType(com.yigongbao.module.notification.announcement.enums.AnnouncementTargetTypeEnum.valueOf(entity.getTargetType()));
        target.setRoleIds(JSONUtil.toList(entity.getTargetRoleIds(), Long.class));
        target.setUserIds(JSONUtil.toList(entity.getTargetUserIds(), Long.class));
        List<AnnouncementTargetUserVO> users = targetResolver.resolve(target);

        entity.setStatus(AnnouncementStatusEnum.PUBLISHED.getCode());
        entity.setPublishedAt(LocalDateTime.now());
        entity.setPublishedBy(operatorId);
        entity.setTargetCount(users.size());
        entity.setUpdateBy(operatorId);
        announcementMapper.updateById(entity);

        List<SystemAnnouncementRecipientEntity> recipients = new ArrayList<>(users.size());
        for (AnnouncementTargetUserVO user : users) {
            SystemAnnouncementRecipientEntity recipient = new SystemAnnouncementRecipientEntity();
            recipient.setAnnouncementId(id);
            recipient.setUserId(user.getUserId());
            recipient.setUserNameSnapshot(user.getRealName());
            recipient.setUsernameSnapshot(user.getUsername());
            recipient.setRoleSnapshot(user.getRoleName());
            recipient.setDeliveryStatus(AnnouncementRecipientStatusEnum.PENDING.getCode());
            recipient.setCreateBy(operatorId);
            recipient.setUpdateBy(operatorId);
            recipient.setCreateTime(LocalDateTime.now());
            recipient.setUpdateTime(LocalDateTime.now());
            recipient.setIsDeleted(0);
            recipients.add(recipient);
        }
        for (int start = 0; start < recipients.size(); start += 500) {
            int end = Math.min(start + 500, recipients.size());
            recipientMapper.insertBatch(recipients.subList(start, end));
        }

        saveAudit(id, AnnouncementAuditOperationEnum.PUBLISH, operatorId, entity.getTargetType(), users.size(), clientIp);
        pushPublishedAfterCommit(users.stream().map(AnnouncementTargetUserVO::getUserId).toList(), id, entity.getPublishedAt());
        log.info("系统公告发布: announcementId={}, operatorId={}, targetCount={}", id, operatorId, users.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id, Long operatorId, String clientIp) {
        SystemAnnouncementEntity entity = announcementMapper.selectForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        ensureStatus(entity, AnnouncementStatusEnum.PUBLISHED);
        List<Long> recipientUserIds = recipientMapper.selectList(new LambdaQueryWrapper<SystemAnnouncementRecipientEntity>()
                .select(SystemAnnouncementRecipientEntity::getUserId)
                .eq(SystemAnnouncementRecipientEntity::getAnnouncementId, id))
                .stream()
                .map(SystemAnnouncementRecipientEntity::getUserId)
                .distinct()
                .toList();
        int revokedCount = recipientMapper.revokePending(id);
        entity.setStatus(AnnouncementStatusEnum.REVOKED.getCode());
        entity.setRevokedAt(LocalDateTime.now());
        entity.setRevokedBy(operatorId);
        entity.setRevokedCount(revokedCount);
        entity.setUpdateBy(operatorId);
        announcementMapper.updateById(entity);
        saveAudit(id, AnnouncementAuditOperationEnum.REVOKE, operatorId, entity.getTargetType(), entity.getTargetCount(), clientIp);
        pushRevokedAfterCommit(recipientUserIds, id);
        log.info("系统公告撤回: announcementId={}, operatorId={}, revokedCount={}", id, operatorId, revokedCount);
    }

    @Override
    public List<AnnouncementPendingVO> listPending(Long userId) {
        List<AnnouncementPendingVO> pending = recipientMapper.selectPendingByUserId(userId);
        if (pending.isEmpty()) return pending;
        List<Long> announcementIds = pending.stream().map(AnnouncementPendingVO::getId).toList();
        var attachmentsByAnnouncement = attachmentMapper.selectList(new LambdaQueryWrapper<SystemAnnouncementAttachmentEntity>()
                        .in(SystemAnnouncementAttachmentEntity::getAnnouncementId, announcementIds)
                        .orderByAsc(SystemAnnouncementAttachmentEntity::getSort)
                        .orderByAsc(SystemAnnouncementAttachmentEntity::getId))
                .stream().collect(java.util.stream.Collectors.groupingBy(
                        SystemAnnouncementAttachmentEntity::getAnnouncementId,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(this::toAttachmentVO, java.util.stream.Collectors.toList())));
        pending.forEach(announcement -> announcement.setAttachments(
                attachmentsByAnnouncement.getOrDefault(announcement.getId(), List.of())));
        return pending;
    }

    @Override
    public AnnouncementPendingVO getPending(Long announcementId, Long userId) {
        return listPending(userId).stream()
                .filter(item -> announcementId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "公告不存在或当前用户无权查看"));
    }

    @Override
    public int estimateTarget(AnnouncementTargetDTO target) {
        return targetResolver.resolve(target).size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledge(Long announcementId, Long userId) {
        int affected = recipientMapper.acknowledge(announcementId, userId);
        if (affected == 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "公告不存在、已撤回或已确认");
        }
        announcementMapper.update(null, new LambdaUpdateWrapper<SystemAnnouncementEntity>()
                .eq(SystemAnnouncementEntity::getId, announcementId)
                .setSql("acknowledged_count = acknowledged_count + 1"));
    }

    @Override
    public AnnouncementStatisticsVO statistics(Long id) {
        if (announcementMapper.selectById(id) == null) throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        AnnouncementStatisticsVO vo = new AnnouncementStatisticsVO();
        long target = recipientMapper.selectCount(new LambdaQueryWrapper<SystemAnnouncementRecipientEntity>()
                .eq(SystemAnnouncementRecipientEntity::getAnnouncementId, id));
        long pending = recipientMapper.selectCount(new LambdaQueryWrapper<SystemAnnouncementRecipientEntity>()
                .eq(SystemAnnouncementRecipientEntity::getAnnouncementId, id)
                .eq(SystemAnnouncementRecipientEntity::getDeliveryStatus, AnnouncementRecipientStatusEnum.PENDING.getCode()));
        long acknowledged = recipientMapper.selectCount(new LambdaQueryWrapper<SystemAnnouncementRecipientEntity>()
                .eq(SystemAnnouncementRecipientEntity::getAnnouncementId, id)
                .eq(SystemAnnouncementRecipientEntity::getDeliveryStatus, AnnouncementRecipientStatusEnum.ACKNOWLEDGED.getCode()));
        long revoked = recipientMapper.selectCount(new LambdaQueryWrapper<SystemAnnouncementRecipientEntity>()
                .eq(SystemAnnouncementRecipientEntity::getAnnouncementId, id)
                .eq(SystemAnnouncementRecipientEntity::getDeliveryStatus, AnnouncementRecipientStatusEnum.REVOKED.getCode()));
        vo.setTargetCount(target);
        vo.setPendingCount(pending);
        vo.setAcknowledgedCount(acknowledged);
        vo.setRevokedCount(revoked);
        vo.setAcknowledgeRate(target == 0 ? 0 : (int) Math.round(acknowledged * 100.0 / target));
        return vo;
    }

    @Override
    public IPage<AnnouncementRecipientVO> recipients(Long id, AnnouncementRecipientPageDTO query) {
        get(id);
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<SystemAnnouncementRecipientEntity> wrapper = new LambdaQueryWrapper<SystemAnnouncementRecipientEntity>()
                .eq(SystemAnnouncementRecipientEntity::getAnnouncementId, id)
                .eq(StringUtils.hasText(query.getDeliveryStatus()), SystemAnnouncementRecipientEntity::getDeliveryStatus, query.getDeliveryStatus())
                .and(StringUtils.hasText(query.getKeyword()), w -> w.like(SystemAnnouncementRecipientEntity::getUserNameSnapshot, query.getKeyword())
                        .or().like(SystemAnnouncementRecipientEntity::getUsernameSnapshot, query.getKeyword()))
                .orderByDesc(SystemAnnouncementRecipientEntity::getAcknowledgedAt)
                .orderByAsc(SystemAnnouncementRecipientEntity::getId);
        IPage<SystemAnnouncementRecipientEntity> entityPage = recipientMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return entityPage.convert(this::toRecipientVO);
    }

    @Override
    public IPage<SystemAnnouncementAuditLogEntity> auditLogs(Long id, Integer pageNum, Integer pageSize) {
        get(id);
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        return auditLogMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SystemAnnouncementAuditLogEntity>()
                .eq(SystemAnnouncementAuditLogEntity::getAnnouncementId, id)
                .orderByDesc(SystemAnnouncementAuditLogEntity::getOperationTime)
                .orderByDesc(SystemAnnouncementAuditLogEntity::getId));
    }

    private void replaceAttachments(Long announcementId, List<AnnouncementAttachmentDTO> attachments, Long operatorId) {
        attachmentMapper.delete(new LambdaQueryWrapper<SystemAnnouncementAttachmentEntity>()
                .eq(SystemAnnouncementAttachmentEntity::getAnnouncementId, announcementId));
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        for (AnnouncementAttachmentDTO dto : attachments) {
            SystemAnnouncementAttachmentEntity entity = new SystemAnnouncementAttachmentEntity();
            entity.setAnnouncementId(announcementId);
            entity.setFileId(dto.getFileId());
            entity.setFileName(dto.getFileName());
            entity.setFileUrl(dto.getFileUrl());
            entity.setFileType(dto.getFileType());
            entity.setFileSize(dto.getFileSize());
            entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
            entity.setCreateBy(operatorId);
            entity.setUpdateBy(operatorId);
            attachmentMapper.insert(entity);
        }
    }

    /** 附件允许外部 HTTP(S) 地址和站内绝对路径，拒绝 javascript/data 等可执行协议。 */
    private void validateAttachmentUrls(List<AnnouncementAttachmentDTO> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        for (AnnouncementAttachmentDTO attachment : attachments) {
            String value = attachment.getFileUrl() == null ? "" : attachment.getFileUrl().trim();
            boolean internalPath = value.startsWith("/") && !value.startsWith("//");
            boolean externalUrl = false;
            if (!internalPath && !value.isEmpty()) {
                try {
                    URI uri = new URI(value);
                    String scheme = uri.getScheme();
                    externalUrl = ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                            && StringUtils.hasText(uri.getRawAuthority());
                } catch (URISyntaxException ignored) {
                    // 统一转换为业务参数错误，避免将 URI 解析细节暴露给调用方。
                }
            }
            if (!internalPath && !externalUrl) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                        "附件地址仅支持 http(s) 或站内绝对路径");
            }
            attachment.setFileUrl(value);
        }
    }

    private AnnouncementAttachmentVO toAttachmentVO(SystemAnnouncementAttachmentEntity entity) {
        AnnouncementAttachmentVO vo = new AnnouncementAttachmentVO();
        vo.setId(entity.getId());
        vo.setFileId(entity.getFileId());
        vo.setFileName(entity.getFileName());
        vo.setFileUrl(entity.getFileUrl());
        vo.setFileType(entity.getFileType());
        vo.setFileSize(entity.getFileSize());
        vo.setSort(entity.getSort());
        return vo;
    }

    private AnnouncementDetailVO toDetailVO(SystemAnnouncementEntity entity) {
        AnnouncementDetailVO vo = new AnnouncementDetailVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContentHtml(entity.getContentHtml());
        vo.setContentText(entity.getContentText());
        vo.setStatus(entity.getStatus());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetRoleIds(entity.getTargetRoleIds());
        vo.setTargetUserIds(entity.getTargetUserIds());
        vo.setForceConfirm(entity.getForceConfirm());
        vo.setTargetCount(entity.getTargetCount());
        vo.setAcknowledgedCount(entity.getAcknowledgedCount());
        vo.setRevokedCount(entity.getRevokedCount());
        vo.setPublishedAt(entity.getPublishedAt());
        vo.setPublishedBy(entity.getPublishedBy());
        vo.setRevokedAt(entity.getRevokedAt());
        vo.setRevokedBy(entity.getRevokedBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setCreateBy(entity.getCreateBy());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setUpdateBy(entity.getUpdateBy());
        vo.setAttachments(attachmentMapper.selectList(new LambdaQueryWrapper<SystemAnnouncementAttachmentEntity>()
                        .eq(SystemAnnouncementAttachmentEntity::getAnnouncementId, entity.getId())
                        .orderByAsc(SystemAnnouncementAttachmentEntity::getSort)
                        .orderByAsc(SystemAnnouncementAttachmentEntity::getId))
                .stream().map(this::toAttachmentVO).toList());
        return vo;
    }

    private AnnouncementRecipientVO toRecipientVO(SystemAnnouncementRecipientEntity entity) {
        AnnouncementRecipientVO vo = new AnnouncementRecipientVO();
        vo.setId(entity.getId());
        vo.setAnnouncementId(entity.getAnnouncementId());
        vo.setUserId(entity.getUserId());
        vo.setUserNameSnapshot(entity.getUserNameSnapshot());
        vo.setUsernameSnapshot(entity.getUsernameSnapshot());
        vo.setRoleSnapshot(entity.getRoleSnapshot());
        vo.setDeliveryStatus(entity.getDeliveryStatus());
        vo.setAcknowledgedAt(entity.getAcknowledgedAt());
        vo.setRevokedAt(entity.getRevokedAt());
        return vo;
    }

    private void saveAudit(Long announcementId, AnnouncementAuditOperationEnum operation,
                           Long operatorId, String targetType, int targetCount, String clientIp) {
        SystemAnnouncementAuditLogEntity logEntity = new SystemAnnouncementAuditLogEntity();
        logEntity.setAnnouncementId(announcementId);
        logEntity.setOperationType(operation.getCode());
        logEntity.setOperatorId(operatorId);
        logEntity.setBeforeStatus(operation == AnnouncementAuditOperationEnum.PUBLISH
                ? AnnouncementStatusEnum.DRAFT.getCode() : AnnouncementStatusEnum.PUBLISHED.getCode());
        logEntity.setAfterStatus(operation == AnnouncementAuditOperationEnum.PUBLISH
                ? AnnouncementStatusEnum.PUBLISHED.getCode() : AnnouncementStatusEnum.REVOKED.getCode());
        logEntity.setTargetType(targetType);
        logEntity.setTargetCount(targetCount);
        logEntity.setOperatorName(targetResolver.operatorName(operatorId));
        logEntity.setClientIp(clientIp);
        logEntity.setOperationTime(LocalDateTime.now());
        logEntity.setCreateBy(operatorId);
        logEntity.setUpdateBy(operatorId);
        auditLogMapper.insert(logEntity);
    }

    private void pushPublishedAfterCommit(List<Long> userIds, Long announcementId, LocalDateTime publishedAt) {
        Runnable task = () -> userIds.forEach(userId ->
                notificationPushService.pushAnnouncementPublished(userId, announcementId, publishedAt));
        registerAfterCommit(task);
    }

    private void pushRevokedAfterCommit(List<Long> userIds, Long announcementId) {
        Runnable task = () -> userIds.forEach(userId ->
                notificationPushService.pushAnnouncementRevoked(userId, announcementId));
        registerAfterCommit(task);
    }

    private void registerAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void ensureStatus(SystemAnnouncementEntity entity, AnnouncementStatusEnum expected) {
        if (!expected.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "当前公告状态不允许执行此操作：" + entity.getStatus());
        }
    }
}
