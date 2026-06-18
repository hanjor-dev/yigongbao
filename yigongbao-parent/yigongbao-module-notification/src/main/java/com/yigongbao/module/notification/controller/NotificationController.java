package com.yigongbao.module.notification.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.notification.dto.BatchReadDTO;
import com.yigongbao.module.notification.dto.MessageQueryDTO;
import com.yigongbao.module.notification.service.INotificationService;
import com.yigongbao.module.notification.vo.MessageVO;
import com.yigongbao.module.notification.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 消息通知控制器
 * 提供消息查询、已读标记、弹窗确认、删除等接口
 * 所有接口的接收人均从 SaToken 会话中获取，禁止前端传入
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Tag(name = "消息通知", description = "消息查询、已读标记、弹窗确认等接口")
@RestController
@RequestMapping("/notification/messages")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * 分页查询消息列表
     */
    @Operation(summary = "分页查询消息列表", description = "支持按分类、已读状态、消息类型筛选，分页返回，额外携带未读数")
    @PostMapping
    public Result<IPage<MessageVO>> listMessages(@RequestBody @Valid MessageQueryDTO query) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        return Result.success(notificationService.listMessages(query, receiverId));
    }

    /**
     * 获取未读消息数量
     */
    @Operation(summary = "获取未读消息数量", description = "返回总未读数及各分类未读数")
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount() {
        Long receiverId = StpUtil.getLoginIdAsLong();
        return Result.success(notificationService.getUnreadCount(receiverId));
    }

    /**
     * 标记单条消息为已读
     */
    @Operation(summary = "标记单条消息为已读")
    @Parameter(name = "id", description = "消息ID", required = true)
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        notificationService.markRead(id, receiverId);
        return Result.success();
    }

    /**
     * 批量标记已读
     */
    @Operation(summary = "批量标记已读", description = "可按ID列表批量标记，或按分类全部标记（markAll=true）")
    @PutMapping("/batch-read")
    public Result<Void> batchMarkRead(@RequestBody BatchReadDTO dto) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        notificationService.batchMarkRead(dto.getIds(), dto.getCategory(), dto.getMarkAll(), receiverId);
        return Result.success();
    }

    /**
     * 确认弹窗消息
     */
    @Operation(summary = "确认弹窗消息", description = "将 POPUP 类型消息标记为已确认，同时自动标记已读")
    @Parameter(name = "id", description = "消息ID", required = true)
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        notificationService.confirm(id, receiverId);
        return Result.success();
    }

    /**
     * 删除消息（逻辑删除）
     */
    @Operation(summary = "删除消息", description = "逻辑删除，删除后不再出现在消息列表中")
    @Parameter(name = "id", description = "消息ID", required = true)
    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        Long receiverId = StpUtil.getLoginIdAsLong();
        notificationService.deleteMessage(id, receiverId);
        return Result.success();
    }
}
