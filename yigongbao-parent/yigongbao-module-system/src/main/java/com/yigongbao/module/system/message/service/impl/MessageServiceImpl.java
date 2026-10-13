package com.yigongbao.module.system.message.service.impl;

import com.yigongbao.module.system.message.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务实现类（临时桩实现）
 *
 * TODO: 需要集成 yigongbao-module-notification 的 INotificationService
 * 当前仅记录日志，实际消息发送需要在具体业务模块中通过 INotificationService 实现
 *
 * @author hanjor
 * @date 2026-07-10
 */
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Override
    public void sendToUser(Long userId, String title, String content, String linkUrl, Long linkParam) {
        log.info("发送消息给用户: userId={}, title={}, content={}, linkUrl={}, linkParam={}",
                userId, title, content, linkUrl, linkParam);
        // TODO: 集成 INotificationService 实现实际消息发送
    }

    @Override
    public void sendToUsers(List<Long> userIds, String title, String content, String linkUrl, Long linkParam) {
        log.info("批量发送消息: userIds={}, title={}, content={}, linkUrl={}, linkParam={}",
                userIds, title, content, linkUrl, linkParam);
        // TODO: 集成 INotificationService 实现实际消息发送
    }
}
