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

    private static final String AUTH      = "AUTH";
    private static final String PING      = "PING";
    private static final String AUTH_OK   = "{\"type\":\"AUTH_OK\"}";
    private static final String AUTH_FAIL = "{\"type\":\"AUTH_FAIL\"}";
    private static final String PONG      = "{\"type\":\"PONG\"}";

    private final WebSocketSessionManager sessionManager;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject json = JSONUtil.parseObj(message.getPayload());
        String type = json.getStr("type");

        if (AUTH.equals(type)) {
            String token = json.getStr("token");
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    session.sendMessage(new TextMessage(AUTH_FAIL));
                    session.close();
                    return;
                }
                Long userId = Long.parseLong(loginId.toString());
                sessionManager.add(userId, session);
                session.getAttributes().put("userId", userId);
                session.sendMessage(new TextMessage(AUTH_OK));
                log.info("WebSocket 认证成功: userId={}", userId);
            } catch (Exception e) {
                log.warn("WebSocket 认证失败: {}", e.getMessage());
                session.sendMessage(new TextMessage(AUTH_FAIL));
                session.close();
            }
        } else if (PING.equals(type)) {
            session.sendMessage(new TextMessage(PONG));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.remove(userId);
            log.info("WebSocket 断开连接: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.warn("WebSocket 传输异常: userId={}, error={}", userId, exception.getMessage());
    }
}
