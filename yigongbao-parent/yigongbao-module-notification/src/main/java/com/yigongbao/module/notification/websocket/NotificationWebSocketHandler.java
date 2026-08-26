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

                // 注册新会话前，先踢出旧会话
                WebSocketSession oldSession = sessionManager.get(userId);
                if (oldSession != null && oldSession.isOpen() && !oldSession.getId().equals(session.getId())) {
                    try {
                        String kickoutMsg = "{\"type\":\"SESSION_KICKOUT\",\"title\":\"账号在另一处登录\",\"content\":\"您的账号在另一台设备登录，请重新登录\"}";
                        oldSession.sendMessage(new TextMessage(kickoutMsg));
                        oldSession.close(CloseStatus.POLICY_VIOLATION);
                        log.info("踢出旧会话: userId={}, oldSessionId={}", userId, oldSession.getId());
                    } catch (Exception e) {
                        log.warn("踢出旧会话失败: userId={}", userId, e);
                    }
                }

                sessionManager.add(userId, session);
                session.getAttributes().put("userId", userId);
                sendAuthSuccess(session, userId);
                log.info("WebSocket 认证成功: userId={}, sessionId={}", userId, session.getId());
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
            // 只有当前会话是该用户的活动会话时才移除（避免误删新会话）
            WebSocketSession currentSession = sessionManager.get(userId);
            if (currentSession != null && currentSession.getId().equals(session.getId())) {
                sessionManager.remove(userId);
                log.info("WebSocket 断开连接并移除会话: userId={}, sessionId={}, status={}", userId, session.getId(), status);
            } else {
                log.info("WebSocket 断开连接（旧会话）: userId={}, sessionId={}, status={}", userId, session.getId(), status);
            }
        } else {
            log.debug("WebSocket 未认证连接断开: sessionId={}, status={}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.warn("WebSocket 传输异常: userId={}, error={}", userId, exception.getMessage());
    }
}
