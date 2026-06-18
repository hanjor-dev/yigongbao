package com.yigongbao.module.basic.device.websocket;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
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
    private final ProcessingCenterMapper processingCenterMapper;
    private final IProcessingCenterService processingCenterService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("接收到加工中心推送数据: sessionId={}, payload={}", session.getId(), payload);

        try {
            DeviceStatusPushDTO dto = JSONUtil.toBean(payload, DeviceStatusPushDTO.class);

            connectionManager.addSession(dto.getCenterName(), session);

            // 查询加工中心并更新连接状态为在线
            ProcessingCenterEntity center = processingCenterMapper.selectOne(
                new LambdaQueryWrapper<ProcessingCenterEntity>()
                    .eq(ProcessingCenterEntity::getCenterName, dto.getCenterName())
                    .last("LIMIT 1"));

            if (center != null) {
                processingCenterService.updateConnectionStatus(center.getId(), 1);
            }

            boolean success = deviceService.batchUpdateDeviceStatus(dto);
            if (success) {
                log.info("处理加工中心推送数据成功: centerName={}, deviceCount={}",
                    dto.getCenterName(), dto.getDevices() != null ? dto.getDevices().size() : 0);
                session.sendMessage(new TextMessage("{\"code\":200,\"message\":\"success\"}"));
            } else {
                log.warn("处理加工中心推送数据失败: centerName={}, reason=加工中心不存在或已禁用", dto.getCenterName());
                session.sendMessage(new TextMessage("{\"code\":404,\"message\":\"加工中心不存在或已禁用\"}"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}, payload={}", session.getId(), payload, e);
            try {
                session.sendMessage(new TextMessage("{\"code\":500,\"message\":\"error\"}"));
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String centerName = connectionManager.getCenterNameBySessionId(session.getId());
        connectionManager.removeSession(session.getId());

        if (centerName != null) {
            ProcessingCenterEntity center = processingCenterMapper.selectOne(
                new LambdaQueryWrapper<ProcessingCenterEntity>()
                    .eq(ProcessingCenterEntity::getCenterName, centerName)
                    .last("LIMIT 1"));
            if (center != null) {
                // 标记加工中心离线
                processingCenterService.updateConnectionStatus(center.getId(), 0);
                // 标记该中心所有设备离线
                deviceService.markDevicesOffline(center.getId());
            }
        }
        log.info("WebSocket连接关闭: sessionId={}, centerName={}, status={}", session.getId(), centerName, status);
    }
}
