package com.yigongbao.module.system.message.service;

import java.util.List;

/**
 * 消息服务接口
 * 提供简化的消息发送功能，用于业务流程中的通知场景
 *
 * @author hanjor
 * @date 2026-07-10
 */
public interface MessageService {

    /**
     * 发送消息给单个用户
     *
     * @param userId   接收人用户ID
     * @param title    消息标题
     * @param content  消息内容
     * @param linkUrl  跳转链接URL（可选）
     * @param linkParam 链接参数（可选，如订单ID）
     */
    void sendToUser(Long userId, String title, String content, String linkUrl, Long linkParam);

    /**
     * 批量发送消息给多个用户
     *
     * @param userIds  接收人用户ID列表
     * @param title    消息标题
     * @param content  消息内容
     * @param linkUrl  跳转链接URL（可选）
     * @param linkParam 链接参数（可选，如订单ID）
     */
    void sendToUsers(List<Long> userIds, String title, String content, String linkUrl, Long linkParam);
}
