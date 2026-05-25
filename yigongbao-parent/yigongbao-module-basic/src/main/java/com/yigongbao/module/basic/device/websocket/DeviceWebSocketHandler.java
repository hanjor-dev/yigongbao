package com.yigongbao.module.basic.device.websocket;

import cn.hutool.json.JSONUtil;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final IDeviceService deviceService;
    private final DeviceConnectionManager connectionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            DeviceStatusPushDTO dto = JSONUtil.toBean(payload, DeviceStatusPushDTO.class);

            connectionManager.addSession(dto.getCenterName(), session);
            deviceService.batchUpdateDeviceStatus(dto);

            session.sendMessage(new TextMessage("{\"code\":200,\"message\":\"success\"}"));
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}", session.getId(), e);
            try {
                session.sendMessage(new TextMessage("{\"code\":500,\"message\":\"error\"}"));
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }
}
