package com.yigongbao.module.notification.service.impl;

import cn.hutool.json.JSONUtil;
import com.yigongbao.module.notification.entity.NotificationMessageEntity;
import com.yigongbao.module.notification.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * WebSocket 推送服务
 * 职责单一：从 WebSocketSessionManager 获取在线会话并推送消息
 * 推送失败仅记录 WARN，不向外抛出异常，避免影响其他用户的推送
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPushService {

    private final WebSocketSessionManager sessionManager;

    /**
     * 批量推送，userIds 与 messages 严格 1:1 对应（由 send() 保证）
     *
     * @param userIds  接收人用户ID列表
     * @param messages 对应的消息实体列表，顺序与 userIds 一致
     */
    public void pushToUsers(List<Long> userIds, List<NotificationMessageEntity> messages) {
        IntStream.range(0, userIds.size()).forEach(i -> pushToUser(userIds.get(i), messages.get(i)));
    }

    /**
     * 向单个在线用户推送 WebSocket 消息
     * 用户离线（无会话或会话已关闭）时静默跳过
     *
     * @param userId  接收人用户ID
     * @param message 消息实体
     */
    public void pushToUser(Long userId, NotificationMessageEntity message) {
        WebSocketSession session = sessionManager.get(userId);
        // 用户不在线，消息已持久化，登录后由前端主动拉取
        if (session == null || !session.isOpen()) {
            log.info("WebSocket 推送跳过（用户不在线）: userId={}, messageId={}", userId, message.getId());
            return;
        }
        try {
            // 仅推送前端弹窗所需字段，详情由前端调查询接口获取
            Map<String, Object> data = new HashMap<>();
            data.put("id", message.getId());
            data.put("messageType", message.getMessageType());
            data.put("category", message.getCategory());
            data.put("title", message.getTitle());
            data.put("content", message.getContent());
            data.put("bizStatus", message.getBizStatus());
            data.put("createTime", message.getCreateTime() != null ? message.getCreateTime().toString() : null);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "NEW_MESSAGE");
            payload.put("data", data);

            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(payload)));
            log.info("WebSocket 推送成功: userId={}, messageId={}, category={}, title={}",
                    userId, message.getId(), message.getCategory(), message.getTitle());
        } catch (Exception e) {
            log.warn("WebSocket 推送失败: userId={}, messageId={}, error={}", userId, message.getId(), e.getMessage());
        }
    }
}
