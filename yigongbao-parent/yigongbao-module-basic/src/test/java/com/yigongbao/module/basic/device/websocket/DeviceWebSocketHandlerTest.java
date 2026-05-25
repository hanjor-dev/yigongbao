package com.yigongbao.module.basic.device.websocket;

import com.yigongbao.module.basic.device.service.IDeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceWebSocketHandlerTest {

    @Mock
    private IDeviceService deviceService;

    @Mock
    private DeviceConnectionManager connectionManager;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private DeviceWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(session.getId()).thenReturn("test-session-id");
    }

    @Test
    void testHandleTextMessage_Success() throws Exception {
        String payload = "{\"center_name\":\"武汉嘉一\",\"devices\":[{\"id\":\"SLA-001\",\"state\":1}]}";
        TextMessage message = new TextMessage(payload);

        handler.handleTextMessage(session, message);

        verify(deviceService, times(1)).batchUpdateDeviceStatus(any());
        verify(connectionManager, times(1)).addSession(anyString(), any());
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testAfterConnectionEstablished() {
        handler.afterConnectionEstablished(session);
        verify(session, times(1)).getId();
    }
}
