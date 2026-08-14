package com.yigongbao.module.basic.device.websocket;

import com.yigongbao.module.basic.device.dto.DeviceStatusPushDTO;
import com.yigongbao.module.basic.device.service.IDeviceService;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceWebSocketHandlerTest {

    @Mock
    private IDeviceService deviceService;

    @Mock
    private DeviceConnectionManager connectionManager;

    @Mock
    private ProcessingCenterMapper processingCenterMapper;

    @Mock
    private IProcessingCenterService processingCenterService;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private DeviceWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        when(session.getId()).thenReturn("test-session-id");
    }

    @Test
    void testHandleTextMessage_Success() throws Exception {
        String payload = "{\"center_name\":\"武汉嘉一\",\"devices\":[{\"id\":\"SLA-001\",\"state\":1}]}";
        TextMessage message = new TextMessage(payload);
        when(deviceService.batchUpdateDeviceStatus(any())).thenReturn(true);

        handler.handleTextMessage(session, message);

        verify(deviceService, times(1)).batchUpdateDeviceStatus(any());
        verify(connectionManager).addSession("武汉嘉一", session);
        verify(session).sendMessage(new TextMessage("{\"code\":200,\"message\":\"success\"}"));
    }

    @Test
    void testHandleTextMessage_AllPrinterStatesReachServiceWithExistingPayloadShape() throws Exception {
        String payload = "{\"center_name\":\"武汉嘉一\",\"devices\":["
            + "{\"id\":\"SLA-000\",\"state\":0},"
            + "{\"id\":\"SLA-001\",\"state\":1},"
            + "{\"id\":\"SLA-002\",\"state\":2},"
            + "{\"id\":\"SLA-003\",\"state\":3},"
            + "{\"id\":\"SLA-004\",\"state\":4},"
            + "{\"id\":\"SLA-005\",\"state\":5},"
            + "{\"id\":\"SLA-006\",\"state\":6}]}";
        when(deviceService.batchUpdateDeviceStatus(any())).thenReturn(true);

        handler.handleTextMessage(session, new TextMessage(payload));

        ArgumentCaptor<DeviceStatusPushDTO> captor = ArgumentCaptor.forClass(DeviceStatusPushDTO.class);
        verify(deviceService).batchUpdateDeviceStatus(captor.capture());
        DeviceStatusPushDTO dto = captor.getValue();
        assertEquals("武汉嘉一", dto.getCenterName());
        assertEquals(List.of("SLA-000", "SLA-001", "SLA-002", "SLA-003", "SLA-004", "SLA-005", "SLA-006"),
            dto.getDevices().stream().map(DeviceStatusPushDTO.DeviceStatus::getId).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6),
            dto.getDevices().stream().map(DeviceStatusPushDTO.DeviceStatus::getState).toList());
        verify(connectionManager).addSession("武汉嘉一", session);
        verify(session).sendMessage(new TextMessage("{\"code\":200,\"message\":\"success\"}"));
    }

    @Test
    void testAfterConnectionEstablished() {
        handler.afterConnectionEstablished(session);
        verify(session, times(1)).getId();
    }
}
