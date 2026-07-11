package com.yigongbao.module.order.listener;

import com.yigongbao.common.constant.RoleCodeConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.event.CancelApplyApprovedEvent;
import com.yigongbao.common.event.CancelApplyRejectedEvent;
import com.yigongbao.common.event.CancelApplySubmittedEvent;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.message.service.MessageService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单取消申请事件监听器
 * 监听取消申请的提交、审核通过、审核驳回事件，并发送消息通知相关用户
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelApplyEventListener {

    private final MessageService messageService;
    private final UserService userService;
    private final OrderMainService orderMainService;
    private final OrderCancelApplyService cancelApplyService;

    /**
     * 处理取消申请提交事件
     * 通知所有设计管理员有新的取消申请需要审核
     *
     * @param event 取消申请提交事件
     */
    @EventListener
    @Async
    public void handleCancelApplySubmitted(CancelApplySubmittedEvent event) {
        if (event == null || event.getApplyId() == null || event.getOrderId() == null) {
            log.warn("事件数据不完整，跳过处理: event={}", event);
            return;
        }

        Long applyId = event.getApplyId();
        Long orderId = event.getOrderId();
        Long applyBy = event.getApplyBy();

        try {
            // 获取订单信息
            OrderMainEntity order = orderMainService.getById(orderId);
            if (order == null) {
                log.warn("处理取消申请提交事件失败，订单不存在: applyId={}, orderId={}", applyId, orderId);
                return;
            }

            // 获取申请信息
            OrderCancelApplyEntity apply = cancelApplyService.getById(applyId);
            if (apply == null) {
                log.warn("处理取消申请提交事件失败，申请不存在: applyId={}", applyId);
                return;
            }

            // 获取申请人姓名
            String applicantName = userService.getUserRealName(applyBy);
            if (applicantName == null) {
                applicantName = "未知用户";
            }

            // 获取所有设计管理员
            List<Long> adminIds = userService.getUserIdsByRoleCode(RoleCodeConstants.DESIGN_ADMIN);
            if (CollUtil.isEmpty(adminIds)) {
                log.warn("处理取消申请提交事件失败，无设计管理员: applyId={}, orderId={}", applyId, orderId);
                return;
            }

            // 构建消息内容
            String title = "新的订单取消申请";
            String reason = StrUtil.blankToDefault(apply.getApplyReason(), "未填写");
            String content = String.format("订单 %s 有新的取消申请，申请人：%s，申请原因：%s",
                    order.getOrderCode(), applicantName, reason);

            // 发送消息给所有设计管理员
            messageService.sendToUsers(adminIds, title, content, "/order/cancel-apply/audit", applyId);

            log.info("取消申请提交事件处理完成: applyId={}, orderId={}, notifyCount={}",
                    applyId, orderId, adminIds.size());

        } catch (Exception e) {
            log.error("处理取消申请提交事件异常: applyId={}, orderId={}", applyId, orderId, e);
        }
    }

    /**
     * 处理取消申请审核通过事件
     * 通知申请人其取消申请已通过，订单已取消
     *
     * @param event 取消申请审核通过事件
     */
    @EventListener
    @Async
    public void handleCancelApplyApproved(CancelApplyApprovedEvent event) {
        if (event == null || event.getApplyId() == null || event.getOrderId() == null) {
            log.warn("事件数据不完整，跳过处理: event={}", event);
            return;
        }

        Long applyId = event.getApplyId();
        Long orderId = event.getOrderId();
        Long auditBy = event.getAuditBy();
        Long applyBy = event.getApplyBy();

        try {
            // 获取订单信息
            OrderMainEntity order = orderMainService.getById(orderId);
            if (order == null) {
                log.warn("处理取消申请通过事件失败，订单不存在: applyId={}, orderId={}", applyId, orderId);
                return;
            }

            // 获取审核人姓名
            String auditorName = userService.getUserRealName(auditBy);
            if (auditorName == null) {
                auditorName = "未知用户";
            }

            // 构建消息内容
            String title = "订单取消申请已通过";
            String content = String.format("您的订单 %s 取消申请已通过，审核人：%s，订单已取消",
                    order.getOrderCode(), auditorName);

            // 发送消息给申请人
            messageService.sendToUser(applyBy, title, content, "/order/detail/{orderId}", orderId);

            log.info("取消申请通过事件处理完成: applyId={}, orderId={}, applyBy={}",
                    applyId, orderId, applyBy);

        } catch (Exception e) {
            log.error("处理取消申请通过事件异常: applyId={}, orderId={}", applyId, orderId, e);
        }
    }

    /**
     * 处理取消申请审核驳回事件
     * 通知申请人其取消申请已被驳回
     *
     * @param event 取消申请审核驳回事件
     */
    @EventListener
    @Async
    public void handleCancelApplyRejected(CancelApplyRejectedEvent event) {
        if (event == null || event.getApplyId() == null || event.getOrderId() == null) {
            log.warn("事件数据不完整，跳过处理: event={}", event);
            return;
        }

        Long applyId = event.getApplyId();
        Long orderId = event.getOrderId();
        Long auditBy = event.getAuditBy();
        Long applyBy = event.getApplyBy();

        try {
            // 获取订单信息
            OrderMainEntity order = orderMainService.getById(orderId);
            if (order == null) {
                log.warn("处理取消申请驳回事件失败，订单不存在: applyId={}, orderId={}", applyId, orderId);
                return;
            }

            // 获取申请信息
            OrderCancelApplyEntity apply = cancelApplyService.getById(applyId);
            if (apply == null) {
                log.warn("处理取消申请驳回事件失败，申请不存在: applyId={}", applyId);
                return;
            }

            // 获取审核人姓名
            String auditorName = userService.getUserRealName(auditBy);
            if (auditorName == null) {
                auditorName = "未知用户";
            }

            // 构建消息内容
            String title = "订单取消申请已驳回";
            String rejectReason = StrUtil.blankToDefault(apply.getAuditReason(), "未填写");
            String content = String.format("您的订单 %s 取消申请已被驳回，审核人：%s，驳回原因：%s",
                    order.getOrderCode(), auditorName, rejectReason);

            // 发送消息给申请人
            messageService.sendToUser(applyBy, title, content, "/order/detail/{orderId}", orderId);

            log.info("取消申请驳回事件处理完成: applyId={}, orderId={}, applyBy={}",
                    applyId, orderId, applyBy);

        } catch (Exception e) {
            log.error("处理取消申请驳回事件异常: applyId={}, orderId={}", applyId, orderId, e);
        }
    }
}
