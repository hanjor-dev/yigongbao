package com.yigongbao.module.notification.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import com.yigongbao.module.notification.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SaToken 会话踢出监听器
 * 通过 WebSocket 推送临时消息，不保存数据库
 *
 * @author hanjor
 * @date 2026-06-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionKickoutListener implements SaTokenListener {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        Long userId = (Long) loginId;

        // 注意：此时 sessionManager 中的会话通常已经是新会话（在 WebSocket AUTH 时已处理）
        // 作为兜底机制：仅记录日志，不主动断开（避免误踢新会话）
        if (sessionManager.isOnline(userId)) {
            log.info("doReplaced 触发时用户在线（新会话已注册）: userId={}", userId);
        } else {
            log.info("doReplaced 触发时用户不在线: userId={}", userId);
        }
    }

    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
    }

    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
    }

    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
    }

    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {
    }

    @Override
    public void doCreateSession(String id) {
    }

    @Override
    public void doLogoutSession(String id) {
    }

    @Override
    public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
    }
}
