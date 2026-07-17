package com.yigongbao.module.notification.listener;

import com.yigongbao.common.constant.RoleCodeConstants;
import com.yigongbao.common.event.CancelApplySubmittedEvent;
import com.yigongbao.module.notification.dto.CancelApplyNotificationData;
import com.yigongbao.module.notification.mapper.CancelApplyQueryMapper;
import com.yigongbao.module.notification.mapper.NotificationMessageMapper;
import com.yigongbao.module.notification.service.INotificationService;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private INotificationService notificationService;

    @Mock
    private NotificationMessageMapper notificationMessageMapper;

    @Mock
    private CancelApplyQueryMapper cancelApplyQueryMapper;

    @Mock
    private IProductionRecordService productionRecordService;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void onCancelApplySubmitted_notifiesAllDesignManagers() {
        CancelApplyNotificationData data = new CancelApplyNotificationData();
        data.setOrderCode("ORD-20260717");
        data.setApplyReason("客户取消");
        when(cancelApplyQueryMapper.findByApplyId(3001L)).thenReturn(data);
        when(userService.getUserRealName(1001L)).thenReturn("申请人");

        listener.onCancelApplySubmitted(new CancelApplySubmittedEvent(this, 3001L, 2001L, 1001L));

        verify(notificationService).send(
                eq(RoleCodeConstants.DESIGN_ADMIN),
                any(),
                any());
    }
}
