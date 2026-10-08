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

    /**
     * 分页查询公告管理列表。
     * <p>支持按标题、公告状态和推送范围筛选，并按发布时间、公告 ID 倒序排列；草稿未发布时按 ID 保证结果稳定。</p>
     *
     * @param query 分页及筛选条件
     * @return 公告主表分页结果
     */
    @Override
    public IPage<SystemAnnouncementEntity> page(AnnouncementPageDTO query) {
        // 限制分页参数范围，避免异常页码或超大分页造成无效查询。
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

    /**
     * 查询公告详情，并组装按排序号排列的附件信息。
     *
     * @param id 公告主键
     * @return 公告详情；公告不存在时抛出业务异常
     */
    @Override
    public AnnouncementDetailVO get(Long id) {
        SystemAnnouncementEntity entity = announcementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return toDetailVO(entity);
    }

    /**
     * 创建公告草稿。
     * <p>正文会经过统一的安全清洗，同时保存纯文本内容、发布目标配置和附件关联；草稿保存时同步估算当前有效目标人数，正式发布时再按发布时目标快照重新生成接收人记录。</p>
     *
     * @param dto 草稿标题、富文本正文、目标规则和附件
     * @param operatorId 创建人 ID
     * @return 新建公告 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(AnnouncementCreateDTO dto, Long operatorId) {
        // 草稿阶段也必须校验附件地址，避免非法协议进入后续预览或发布流程。
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
        // 草稿保存时即计算当前目标人数，便于后台列表立即展示预计接收人数。
        entity.setTargetCount(estimateTarget(dto.getTarget()));
        entity.setAcknowledgedCount(0);
        entity.setRevokedCount(0);
        entity.setCreateBy(operatorId);
        entity.setUpdateBy(operatorId);
        // 公告主表先落库，生成公告 ID 后再建立附件关联。
        announcementMapper.insert(entity);
        replaceAttachments(entity.getId(), dto.getAttachments(), operatorId);
        return entity.getId();
    }

    /**
     * 更新公告草稿。
     * <p>通过行锁读取公告并校验状态，只允许 DRAFT 状态修改；发布后正文、目标和附件均不可变更。</p>
     *
     * @param id 公告主键
     * @param dto 新的标题、正文、目标规则和附件
     * @param operatorId 修改人 ID
     */
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
        // 目标规则发生变化时同步刷新草稿预计人数，避免列表继续显示旧的统计值。
        entity.setTargetCount(estimateTarget(dto.getTarget()));
        entity.setUpdateBy(operatorId);
        // 使用行锁读取的实体更新，避免并发发布或修改覆盖草稿内容。
        announcementMapper.updateById(entity);
        replaceAttachments(id, dto.getAttachments(), operatorId);
    }

    /**
     * 生成公告预览内容。
     * <p>预览和保存/发布复用同一正文清洗规则，但不会写入公告主表或附件表。</p>
     *
     * @param dto 待预览公告内容
     * @return 清洗后的 HTML 和纯文本预览对象
     */
    @Override
    public AnnouncementPreviewVO preview(AnnouncementCreateDTO dto) {
        validateAttachmentUrls(dto.getAttachments());
        String html = contentSanitizer.sanitize(dto.getContentHtml());
        return new AnnouncementPreviewVO(dto.getTitle().trim(), html, contentSanitizer.toPlainText(html));
    }

    /**
     * 发布公告并创建接收人快照。
     * <p>发布过程会锁定草稿、按发布时的目标规则解析有效账户、批量创建接收记录、写入发布日志；事务提交成功后才发送在线推送。</p>
     *
     * @param id 公告主键
     * @param operatorId 发布人 ID
     * @param clientIp 发布请求来源 IP
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, Long operatorId, String clientIp) {
        // 锁定公告主记录，保证同一草稿不会被并发重复发布。
        SystemAnnouncementEntity entity = announcementMapper.selectForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        ensureStatus(entity, AnnouncementStatusEnum.DRAFT);
        AnnouncementTargetDTO target = new AnnouncementTargetDTO();
        target.setTargetType(com.yigongbao.module.notification.announcement.enums.AnnouncementTargetTypeEnum.valueOf(entity.getTargetType()));
        target.setRoleIds(JSONUtil.toList(entity.getTargetRoleIds(), Long.class));
        target.setUserIds(JSONUtil.toList(entity.getTargetUserIds(), Long.class));
        // 按发布时的目标配置解析有效用户，形成不可变的接收人快照。
        List<AnnouncementTargetUserVO> users = targetResolver.resolve(target);

        entity.setStatus(AnnouncementStatusEnum.PUBLISHED.getCode());
        entity.setPublishedAt(LocalDateTime.now());
        entity.setPublishedBy(operatorId);
        entity.setTargetCount(users.size());
        entity.setUpdateBy(operatorId);
        announcementMapper.updateById(entity);

        // 接收人快照保存姓名、账号和角色，后续用户资料变化不影响历史统计。
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
        // 分批插入，避免全员公告一次性生成过大的 SQL 参数包。
        for (int start = 0; start < recipients.size(); start += 500) {
            int end = Math.min(start + 500, recipients.size());
            recipientMapper.insertBatch(recipients.subList(start, end));
        }

        // 审计日志与公告状态在同一事务内保存，推送则延迟到提交成功后执行。
        saveAudit(id, AnnouncementAuditOperationEnum.PUBLISH, operatorId, entity.getTargetType(), users.size(), clientIp);
        pushPublishedAfterCommit(users.stream().map(AnnouncementTargetUserVO::getUserId).toList(), id, entity.getPublishedAt());
        log.info("系统公告发布: announcementId={}, operatorId={}, targetCount={}", id, operatorId, users.size());
    }

    /**
     * 撤回已发布公告。
     * <p>仅将待确认接收记录变更为 REVOKED，已确认记录保持历史确认事实；事务提交后通知在线用户关闭公告弹窗。</p>
     *
     * @param id 公告主键
     * @param operatorId 撤回人 ID
     * @param clientIp 撤回请求来源 IP
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id, Long operatorId, String clientIp) {
        // 锁定主记录并校验状态，防止重复撤回或撤回草稿。
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
        // 只有待确认记录会被撤回，已确认记录保留其阅读确认事实。
        int revokedCount = recipientMapper.revokePending(id);
        entity.setStatus(AnnouncementStatusEnum.REVOKED.getCode());
        entity.setRevokedAt(LocalDateTime.now());
        entity.setRevokedBy(operatorId);
        entity.setRevokedCount(revokedCount);
        entity.setUpdateBy(operatorId);
        announcementMapper.updateById(entity);
        // 撤回日志必须记录实际公告目标数，便于后台追溯操作。
        saveAudit(id, AnnouncementAuditOperationEnum.REVOKE, operatorId, entity.getTargetType(), entity.getTargetCount(), clientIp);
        pushRevokedAfterCommit(recipientUserIds, id);
        log.info("系统公告撤回: announcementId={}, operatorId={}, revokedCount={}", id, operatorId, revokedCount);
    }

    /**
     * 查询当前用户所有待确认公告。
     * <p>公告按发布时间倒序返回，附件通过一次批量查询后按公告 ID 分组，避免逐条查询产生 N+1 问题。</p>
     *
     * @param userId 当前登录用户 ID
     * @return 当前用户待确认公告队列
     */
    @Override
    public List<AnnouncementPendingVO> listPending(Long userId) {
        List<AnnouncementPendingVO> pending = recipientMapper.selectPendingByUserId(userId);
        if (pending.isEmpty()) return pending;
        // 一次性查询所有附件，避免逐条公告查询造成 N+1 查询。
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

    /**
     * 查询指定用户可见的单条待确认公告。
     *
     * @param announcementId 公告主键
     * @param userId 当前登录用户 ID
     * @return 待确认公告详情
     */
    @Override
    public AnnouncementPendingVO getPending(Long announcementId, Long userId) {
        return listPending(userId).stream()
                .filter(item -> announcementId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "公告不存在或当前用户无权查看"));
    }

    /**
     * 根据目标规则估算当前有效接收人数。
     * <p>该方法只解析目标用户，不创建公告、接收记录或审计日志。</p>
     *
     * @param target 全员、角色或指定账户目标规则
     * @return 当前规则匹配的有效账户数量
     */
    @Override
    public int estimateTarget(AnnouncementTargetDTO target) {
        return targetResolver.estimate(target);
    }

    /**
     * 确认当前用户公告。
     * <p>只有待确认记录成功更新为 ACKNOWLEDGED 后，才会原子增加公告主表的确认人数。</p>
     *
     * @param announcementId 公告主键
     * @param userId 确认人 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledge(Long announcementId, Long userId) {
        int affected = recipientMapper.acknowledge(announcementId, userId);
        if (affected == 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "公告不存在、已撤回或已确认");
        }
        // 只有状态从 PENDING 成功变更为 ACKNOWLEDGED 时才增加统计数。
        announcementMapper.update(null, new LambdaUpdateWrapper<SystemAnnouncementEntity>()
                .eq(SystemAnnouncementEntity::getId, announcementId)
                .setSql("acknowledged_count = acknowledged_count + 1"));
    }

    /**
     * 查询公告确认汇总。
     *
     * @param id 公告主键
     * @return 目标人数、待确认人数、已确认人数、撤回人数和确认率
     */
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

    /**
     * 分页查询公告接收人快照明细。
     * <p>查询由专用 Mapper SQL 完成，明确区分 realName 和 usernameSnapshot，并按接收状态、接收时间、姓名排序。</p>
     *
     * @param id 公告主键
     * @param query 接收状态、关键字及分页条件
     * @return 接收人统计分页结果
     */
    @Override
    public IPage<AnnouncementRecipientVO> recipients(Long id, AnnouncementRecipientPageDTO query) {
        get(id);
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : Math.min(query.getPageSize(), 100);
        return recipientMapper.selectRecipientPage(new Page<>(pageNum, pageSize), id,
                query.getDeliveryStatus(), query.getKeyword());
    }

    /**
     * 分页查询公告发布、撤回审计日志。
     *
     * @param id 公告主键
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页条数，最大 100
     * @return 审计日志分页结果
     */
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

    /**
     * 替换草稿附件关联；空附件集合表示删除该公告的全部附件。
     *
     * @param announcementId 公告主键
     * @param attachments 当前表单提交的附件列表
     * @param operatorId 操作人 ID
     */
    private void replaceAttachments(Long announcementId, List<AnnouncementAttachmentDTO> attachments, Long operatorId) {
        // 先删除旧关联，再插入当前表单提交的附件，保证草稿内容与附件完全一致。
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

    /**
     * 校验附件地址，仅允许外部 HTTP(S) 地址或站内绝对路径，拒绝 javascript/data 等可执行协议。
     */
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

    /**
     * 将附件实体转换为前端展示对象。
     *
     * @param entity 附件实体
     * @return 附件展示对象
     */
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

    /**
     * 将公告主表实体转换为详情对象，并加载按排序号排列的附件。
     *
     * @param entity 公告主表实体
     * @return 公告详情对象
     */
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

    /**
     * 将接收人实体转换为统计接口对象，保留发布时的姓名、账号和角色快照。
     *
     * @param entity 接收人实体
     * @return 接收人统计对象
     */
    private AnnouncementRecipientVO toRecipientVO(SystemAnnouncementRecipientEntity entity) {
        AnnouncementRecipientVO vo = new AnnouncementRecipientVO();
        vo.setId(entity.getId());
        vo.setAnnouncementId(entity.getAnnouncementId());
        vo.setUserId(entity.getUserId());
        // user_name_snapshot 是发布时保存的姓名快照，对外统一命名为 realName。
        vo.setRealName(entity.getUserNameSnapshot());
        vo.setUserNameSnapshot(entity.getUserNameSnapshot());
        vo.setUsernameSnapshot(entity.getUsernameSnapshot());
        vo.setRoleSnapshot(entity.getRoleSnapshot());
        vo.setDeliveryStatus(entity.getDeliveryStatus());
        vo.setAcknowledgedAt(entity.getAcknowledgedAt());
        vo.setRevokedAt(entity.getRevokedAt());
        return vo;
    }

    /**
     * 保存公告发布或撤回审计日志。
     *
     * @param announcementId 公告主键
     * @param operation 审计操作类型
     * @param operatorId 操作人 ID
     * @param targetType 发布目标类型
     * @param targetCount 操作时的目标人数
     * @param clientIp 请求来源 IP
     */
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

    /**
     * 注册发布事件提交后推送任务，避免事务回滚后仍向用户发送通知。
     *
     * @param userIds 接收人用户 ID 列表
     * @param announcementId 公告主键
     * @param publishedAt 发布时间
     */
    private void pushPublishedAfterCommit(List<Long> userIds, Long announcementId, LocalDateTime publishedAt) {
        Runnable task = () -> userIds.forEach(userId ->
                notificationPushService.pushAnnouncementPublished(userId, announcementId, publishedAt));
        registerAfterCommit(task);
    }

    /**
     * 注册撤回事件提交后推送任务，用于关闭用户当前已打开的公告弹窗。
     *
     * @param userIds 接收人用户 ID 列表
     * @param announcementId 公告主键
     */
    private void pushRevokedAfterCommit(List<Long> userIds, Long announcementId) {
        Runnable task = () -> userIds.forEach(userId ->
                notificationPushService.pushAnnouncementRevoked(userId, announcementId));
        registerAfterCommit(task);
    }

    /**
     * 注册事务提交回调；无事务场景下立即执行任务。
     *
     * @param task 事务提交后执行的推送任务
     */
    private void registerAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        // 只有数据库事务成功提交后才允许执行外部推送。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    /**
     * 校验公告当前状态是否满足指定操作的前置条件。
     *
     * @param entity 待校验公告
     * @param expected 操作要求的公告状态
     */
    private void ensureStatus(SystemAnnouncementEntity entity, AnnouncementStatusEnum expected) {
        if (!expected.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "当前公告状态不允许执行此操作：" + entity.getStatus());
        }
    }
}
