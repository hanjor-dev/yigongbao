package com.yigongbao.module.basic.device.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceConnectionManager {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> heartbeats = new ConcurrentHashMap<>();

    public void addSession(String centerName, WebSocketSession session) {
        sessions.put(centerName, session);
        updateHeartbeat(centerName);
    }

    public void removeSession(String centerName) {
        sessions.remove(centerName);
        heartbeats.remove(centerName);
    }

    public void updateHeartbeat(String centerName) {
        heartbeats.put(centerName, LocalDateTime.now());
    }

    public WebSocketSession getSession(String centerName) {
        return sessions.get(centerName);
    }

    public LocalDateTime getLastHeartbeat(String centerName) {
        return heartbeats.get(centerName);
    }

    public Map<String, WebSocketSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }
}
