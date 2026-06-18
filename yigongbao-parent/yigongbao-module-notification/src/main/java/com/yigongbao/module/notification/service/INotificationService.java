package com.yigongbao.module.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.notification.dto.MessageQueryDTO;
import com.yigongbao.module.notification.dto.NotificationContext;
import com.yigongbao.module.notification.dto.NotificationDTO;
import com.yigongbao.module.notification.vo.MessageVO;
import com.yigongbao.module.notification.vo.UnreadCountVO;

import java.util.List;

/**
 * 通知服务接口
 * 提供三种推送重载：按角色、按单用户、按用户列表
 * 所有推送最终收束到 send(List, dto) 统一处理
 *
 * @author hanjor
 * @date 2026-06-18
 */
public interface INotificationService {

    /**
     * 按角色+数据权限范围发送通知
     * 内部读取角色的 dataScopeType，结合 context 过滤目标用户，再调用 send(List, dto)
     *
     * @param roleCode 角色编码（如 production-worker）
     * @param context  数据权限上下文（hospitalId / orgId / centerId）
     * @param dto      消息内容
     */
    void send(String roleCode, NotificationContext context, NotificationDTO dto);

    /**
     * 发送给单个用户
     *
     * @param userId 接收人用户ID
     * @param dto    消息内容
     */
    void send(Long userId, NotificationDTO dto);

    /**
     * 批量发送给指定用户列表（最终收束点）
     * 持久化消息记录后向在线用户推送 WebSocket 消息
     *
     * @param userIds 接收人用户ID列表
     * @param dto     消息内容
     */
    void send(List<Long> userIds, NotificationDTO dto);

    /**
     * 解析角色+数据权限范围对应的用户ID列表
     * 供 Listener 在同一 center 内复用用户查询结果（避免 N+1）
     *
     * @param roleCode 角色编码
     * @param context  数据权限上下文
     * @return 目标用户ID列表，空列表表示无匹配用户
     */
    List<Long> resolveUserIds(String roleCode, NotificationContext context);

    /**
     * 分页查询当前用户的消息列表
     *
     * @param query      查询条件（分类、已读状态、消息类型、分页）
     * @param receiverId 当前登录用户ID（由 Controller 从 SaToken 获取）
     * @return 分页结果
     */
    IPage<MessageVO> listMessages(MessageQueryDTO query, Long receiverId);

    /**
     * 获取未读消息数（总数 + 按分类细分）
     *
     * @param receiverId 当前登录用户ID
     * @return 未读数量 VO，包含 total 和 byCategory
     */
    UnreadCountVO getUnreadCount(Long receiverId);

    /**
     * 标记单条消息为已读
     *
     * @param id         消息ID
     * @param receiverId 当前登录用户ID（防止越权）
     */
    void markRead(Long id, Long receiverId);

    /**
     * 批量/全部标记已读
     * markAll=true 时忽略 ids，按 category 或全部标记
     *
     * @param ids        消息ID列表（markAll=false 时有效）
     * @param category   消息分类（markAll=true 时按分类标记，为 null 则全部标记）
     * @param markAll    是否全部标记
     * @param receiverId 当前登录用户ID
     */
    void batchMarkRead(List<Long> ids, String category, Boolean markAll, Long receiverId);

    /**
     * 确认弹窗消息（POPUP 专用）
     * 同时将消息标记为已读和已确认
     *
     * @param id         消息ID
     * @param receiverId 当前登录用户ID
     */
    void confirm(Long id, Long receiverId);

    /**
     * 删除消息（逻辑删除）
     *
     * @param id         消息ID
     * @param receiverId 当前登录用户ID（防止越权）
     */
    void deleteMessage(Long id, Long receiverId);
}
