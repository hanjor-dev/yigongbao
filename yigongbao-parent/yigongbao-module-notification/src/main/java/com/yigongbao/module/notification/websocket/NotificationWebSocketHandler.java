package com.yigongbao.module.notification.websocket;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 通知 WebSocket 处理器
 * 认证流程：客户端连接后发送首帧 {"type":"AUTH","token":"xxxx"}，验证成功后注册会话
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final String AUTH = "AUTH";
    private static final String PING = "PING";

    private final WebSocketSessionManager sessionManager;
    private final com.yigongbao.module.notification.service.INotificationService notificationService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 连接建立: sessionId={}, remoteAddress={}", session.getId(), session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject json = JSONUtil.parseObj(message.getPayload());
        String type = json.getStr("type");

        if (AUTH.equals(type)) {
            String token = json.getStr("token");
            Long userId = null;
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    sendAuthFailed(session, "Token无效或已过期");
                    return;
                }
                userId = Long.parseLong(loginId.toString());
                sessionManager.add(userId, session);
                session.getAttributes().put("userId", userId);
                sendAuthSuccess(session, userId);
                log.info("WebSocket 认证成功: userId={}", userId);
            } catch (Exception e) {
                log.warn("WebSocket 认证失败: {}", e.getMessage());
                sendAuthFailed(session, "Token无效或已过期");
                return;
            }
            try {
                notificationService.pushPendingNotifications(userId);
            } catch (Exception e) {
                log.warn("补推离线通知失败: userId={}, error={}", userId, e.getMessage());
            }
        } else if (PING.equals(type)) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    private void sendAuthSuccess(WebSocketSession session, Long userId) throws Exception {
        JSONObject response = new JSONObject();
        response.set("type", "AUTH_SUCCESS");
        response.set("userId", userId);
        session.sendMessage(new TextMessage(response.toString()));
    }

    private void sendAuthFailed(WebSocketSession session, String message) throws Exception {
        JSONObject response = new JSONObject();
        response.set("type", "AUTH_FAILED");
        response.set("message", message);
        response.set("code", 401);
        session.sendMessage(new TextMessage(response.toString()));
        session.close(CloseStatus.POLICY_VIOLATION);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.remove(userId);
            log.info("WebSocket 断开连接: userId={}, sessionId={}, status={}", userId, session.getId(), status);
        } else {
            log.info("WebSocket 未认证连接断开: sessionId={}, status={}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.warn("WebSocket 传输异常: userId={}, error={}", userId, exception.getMessage());
    }
}
