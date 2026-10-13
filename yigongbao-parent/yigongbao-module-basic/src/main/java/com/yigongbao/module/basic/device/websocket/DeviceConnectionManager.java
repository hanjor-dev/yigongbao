package com.yigongbao.module.basic.device.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceConnectionManager {

    /** centerName → session */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** sessionId → centerName，用于断连时反查 */
    private final Map<String, String> sessionCenterMap = new ConcurrentHashMap<>();

    public void addSession(String centerName, WebSocketSession session) {
        sessions.put(centerName, session);
        sessionCenterMap.put(session.getId(), centerName);
    }

    public void removeSession(String sessionId) {
        String centerName = sessionCenterMap.remove(sessionId);
        if (centerName != null) {
            sessions.remove(centerName);
        }
    }

    /** 根据 sessionId 反查 centerName，断连时使用 */
    public String getCenterNameBySessionId(String sessionId) {
        return sessionCenterMap.get(sessionId);
    }

    public WebSocketSession getSession(String centerName) {
        return sessions.get(centerName);
    }

    public Map<String, WebSocketSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }
}

