package com.yigongbao.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.notification.dto.MessageQueryDTO;
import com.yigongbao.module.notification.dto.NotificationContext;
import com.yigongbao.module.notification.dto.NotificationDTO;
import com.yigongbao.module.notification.entity.NotificationMessageEntity;
import com.yigongbao.module.notification.mapper.NotificationMessageMapper;
import com.yigongbao.module.notification.mapper.UserQueryMapper;
import com.yigongbao.module.notification.service.INotificationService;
import com.yigongbao.module.notification.vo.MessageVO;
import com.yigongbao.module.notification.vo.UnreadCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * 三个 send 重载最终都收束到 send(List, dto)，统一完成持久化和 WebSocket 推送
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMessageMapper, NotificationMessageEntity>
        implements INotificationService {

    private final UserQueryMapper userQueryMapper;
    private final NotificationPushService pushService;

    /**
     * 按角色+数据权限范围发送通知
     * 先解析目标用户列表，再委托给 send(List, dto)
     */
    @Override
    public void send(String roleCode, NotificationContext context, NotificationDTO dto) {
        List<Long> userIds = resolveUserIds(roleCode, context);
        if (!userIds.isEmpty()) {
            send(userIds, dto);
        }
    }

    /**
     * 解析角色+数据权限范围对应的用户ID列表
     * 根据 sys_role.data_scope_type 分支，查询满足 context 过滤条件的用户
     */
    @Override
    public List<Long> resolveUserIds(String roleCode, NotificationContext context) {
        String scopeType = userQueryMapper.findDataScopeTypeByRoleCode(roleCode);
        DataScopeTypeEnum scope = DataScopeTypeEnum.getByCodeOrDefault(scopeType);
        return switch (scope) {
            // ALL：查询该角色下全部用户，不限机构
            case ALL -> userQueryMapper.findUserIdsByRoleAll(roleCode);
            // ORG/DEPT：查询该角色在指定机构内的用户
            case ORG, DEPT -> {
                if (context.getOrgId() == null) {
                    log.warn("ORG/DEPT scope 缺少 orgId: roleCode={}", roleCode);
                    yield Collections.emptyList();
                }
                yield userQueryMapper.findUserIdsByRoleAndOrg(roleCode, context.getOrgId());
            }
            // CENTER：查询该角色在指定加工中心内的用户
            case CENTER -> {
                if (context.getCenterId() == null) {
                    log.warn("CENTER scope 缺少 centerId: roleCode={}", roleCode);
                    yield Collections.emptyList();
                }
                yield userQueryMapper.findUserIdsByRoleAndCenter(roleCode, context.getCenterId());
            }
            // HOSPITALS：查询该角色关联指定医院的用户（通过 sys_user_hospital 关联表）
            case HOSPITALS -> {
                if (context.getHospitalId() == null) {
                    log.warn("HOSPITALS scope 缺少 hospitalId: roleCode={}", roleCode);
                    yield Collections.emptyList();
                }
                yield userQueryMapper.findUserIdsByRoleAndHospital(roleCode, context.getHospitalId());
            }
            // SELF：不适用角色推送，应改用 send(userId, dto)
            default -> {
                log.warn("SELF scope 不适用角色推送，请改用 send(userId, dto): roleCode={}", roleCode);
                yield Collections.emptyList();
            }
        };
    }

    /**
     * 发送给单个用户，包装后委托给 send(List, dto)
     */
    @Override
    public void send(Long userId, NotificationDTO dto) {
        send(Collections.singletonList(userId), dto);
    }

    /**
     * 最终收束点：批量持久化消息记录，再推送 WebSocket
     * 事务范围仅包含 saveBatch，推送在事务提交后执行，推送失败不回滚消息写入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(List<Long> userIds, NotificationDTO dto) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            log.error("通知 title 为空，跳过发送: category={}, bizId={}", dto.getCategory(), dto.getBizId());
            return;
        }
        // 为每个接收人创建一条独立消息记录
        List<NotificationMessageEntity> entities = userIds.stream()
                .map(uid -> buildEntity(uid, dto))
                .collect(Collectors.toList());
        saveBatch(entities);

        // 注册事务提交后回调，延迟推送到事务提交后执行
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushService.pushToUsers(userIds, entities);
                log.info("通知发送完成: title={}, category={}, type={}, bizType={}, bizId={}, receiverCount={}, receiverIds={}",
                        dto.getTitle(), dto.getCategory(), dto.getMessageType(),
                        dto.getBizType(), dto.getBizId(), userIds.size(), userIds);
            }
        });
    }

    /**
     * 分页查询消息列表，按创建时间倒序
     */
    @Override
    public IPage<MessageVO> listMessages(MessageQueryDTO query, Long receiverId) {
        LambdaQueryWrapper<NotificationMessageEntity> wrapper = new LambdaQueryWrapper<NotificationMessageEntity>()
                .eq(NotificationMessageEntity::getReceiverId, receiverId)
                .eq(query.getCategory() != null, NotificationMessageEntity::getCategory, query.getCategory())
                .eq(query.getIsRead() != null, NotificationMessageEntity::getIsRead, query.getIsRead())
                .eq(query.getIsConfirmed() != null, NotificationMessageEntity::getIsConfirmed, query.getIsConfirmed())
                .eq(query.getMessageType() != null, NotificationMessageEntity::getMessageType, query.getMessageType())
                .orderByDesc(NotificationMessageEntity::getCreateTime);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper).convert(this::toVO);
    }

    /**
     * 获取未读数（总数 + 按分类）
     */
    @Override
    public UnreadCountVO getUnreadCount(Long receiverId) {
        List<Map<String, Object>> rows = baseMapper.selectUnreadCountByCategory(receiverId);
        // 聚合各分类未读数，并计算总数
        Map<String, Long> byCategory = rows.stream().collect(Collectors.toMap(
                r -> (String) r.get("category"),
                r -> ((Number) r.get("count")).longValue(),
                Long::sum,
                LinkedHashMap::new
        ));
        UnreadCountVO vo = new UnreadCountVO();
        vo.setTotal(byCategory.values().stream().mapToLong(Long::longValue).sum());
        vo.setByCategory(byCategory);
        return vo;
    }

    /**
     * 标记单条消息为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id, Long receiverId) {
        baseMapper.batchMarkRead(Collections.singletonList(id), receiverId);
        log.info("标记消息已读: messageId={}, receiverId={}", id, receiverId);
    }

    /**
     * 批量/全部标记已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkRead(List<Long> ids, String category, Boolean markAll, Long receiverId) {
        if (Boolean.TRUE.equals(markAll)) {
            // markAll=true 时，按分类（或全部）批量标记
            baseMapper.markAllRead(receiverId, category);
            log.info("批量标记全部已读: receiverId={}, category={}", receiverId, category);
        } else if (CollectionUtils.isEmpty(ids)) {
            log.warn("批量标记已读参数异常: ids 为空且 markAll=false, receiverId={}", receiverId);
            return;
        } else {
            baseMapper.batchMarkRead(ids, receiverId);
            log.info("批量标记已读: receiverId={}, messageIds={}", receiverId, ids);
        }
    }

    /**
     * 确认弹窗消息，同时标记已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, Long receiverId) {
        update(new LambdaUpdateWrapper<NotificationMessageEntity>()
                .eq(NotificationMessageEntity::getId, id)
                .eq(NotificationMessageEntity::getReceiverId, receiverId)
                .set(NotificationMessageEntity::getIsConfirmed, 1)
                .set(NotificationMessageEntity::getConfirmedTime, LocalDateTime.now())
                .set(NotificationMessageEntity::getIsRead, 1)
                .set(NotificationMessageEntity::getReadTime, LocalDateTime.now()));
        log.info("弹窗消息已确认: messageId={}, receiverId={}", id, receiverId);
    }

    /**
     * 逻辑删除消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long id, Long receiverId) {
        remove(new LambdaQueryWrapper<NotificationMessageEntity>()
                .eq(NotificationMessageEntity::getId, id)
                .eq(NotificationMessageEntity::getReceiverId, receiverId));
        log.info("消息已删除: messageId={}, receiverId={}", id, receiverId);
    }

    /**
     * 构建消息实体
     * 枚举字段（messageType/category）需单独转换为 code，其余字段通过 copyProperties 批量复制
     */
    private NotificationMessageEntity buildEntity(Long userId, NotificationDTO dto) {
        NotificationMessageEntity entity = new NotificationMessageEntity();
        BeanUtils.copyProperties(dto, entity, "messageType", "category");
        entity.setReceiverId(userId);
        entity.setMessageType(dto.getMessageType() != null ? dto.getMessageType().getCode() : null);
        entity.setCategory(dto.getCategory() != null ? dto.getCategory().getCode() : null);
        entity.setIsRead(0);
        entity.setIsConfirmed(0);
        return entity;
    }

    /**
     * 实体转 VO，字段名完全对应，直接 copyProperties
     */
    private MessageVO toVO(NotificationMessageEntity e) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
