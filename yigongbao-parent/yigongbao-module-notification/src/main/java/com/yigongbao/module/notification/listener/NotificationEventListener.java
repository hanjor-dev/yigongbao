package com.yigongbao.module.notification.listener;

import cn.hutool.json.JSONUtil;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.event.*;
import com.yigongbao.module.notification.constant.NotificationJumpUrlConstants;
import com.yigongbao.module.notification.dto.NotificationContext;
import com.yigongbao.module.notification.dto.NotificationDTO;
import com.yigongbao.module.notification.enums.BizTypeEnum;
import com.yigongbao.module.notification.enums.MessageCategoryEnum;
import com.yigongbao.module.notification.enums.MessageTypeEnum;
import com.yigongbao.module.notification.mapper.NotificationMessageMapper;
import com.yigongbao.module.notification.service.INotificationService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 消息通知事件监听器
 * 监听全部业务事件，统一在此处构建消息内容并调用 NotificationService 推送
 * 所有监听方法在事务提交后异步执行，通知失败不影响核心业务
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final INotificationService notificationService;
    private final NotificationMessageMapper notificationMessageMapper;
    private final IProductionRecordService productionRecordService;

    /**
     * 订单提交事件
     * 试用订单 → 通知订单所属部门的区域管理员
     * 其他订单 → 通知全部设计管理员
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        try {
            log.info("收到订单提交事件: orderId={}, businessType={}, deptId={}",
                    event.getOrderId(), event.getBusinessType(), event.getDeptId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("有新的订单待审核")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL + event.getOrderId())
                    .build();
            // 试用订单：通知订单所属部门的区域管理员
            if (DictCodeConstants.ORDER_BUSINESS_TYPE_TRIAL.equals(event.getBusinessType())) {
                log.info("试用订单，通知区域管理员: orderId={}, deptId={}", event.getOrderId(), event.getDeptId());
                notificationService.send(RoleCodeEnum.REGIONAL_MANAGER.getCode(),
                        NotificationContext.ofDept(event.getDeptId()), dto);
            } else {
                // 普通订单：通知全部设计管理员
                log.info("普通订单，通知全部设计管理员: orderId={}", event.getOrderId());
                notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(),
                        NotificationContext.all(), dto);
            }
        } catch (Exception e) {
            log.error("订单提交通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 区域审核通过事件（仅试用订单触发）
     * 通知全部设计管理员
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegionalAuditPassed(RegionalAuditPassedEvent event) {
        try {
            log.info("收到区域审核通过事件: orderId={}", event.getOrderId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("试用订单区域审核已通过，请分配设计师")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL + event.getOrderId())
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(),
                    NotificationContext.all(), dto);
            log.info("已通知全部设计管理员区域审核通过: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("区域审核通过通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 审核驳回事件
     * 定向通知订单创建人
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuditRejected(AuditRejectedEvent event) {
        try {
            log.info("收到审核驳回事件: orderId={}, createBy={}", event.getOrderId(), event.getCreateBy());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("您的订单审核未通过")
                    .content("驳回原因：" + event.getRejectReason())
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.ORDER)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL + event.getOrderId())
                    .build();
            notificationService.send(event.getCreateBy(), dto);
            log.info("已通知订单创建人审核驳回: orderId={}, createBy={}", event.getOrderId(), event.getCreateBy());
        } catch (Exception e) {
            log.error("审核驳回通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 设计师分配事件
     * 通知新设计师（POPUP）；重新分配时同时通知旧设计师（MESSAGE）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDesignerAssigned(DesignerAssignedEvent event) {
        try {
            if (event.getNewDesignerId() == null) {
                log.error("DesignerAssignedEvent.newDesignerId 为 null，跳过通知: orderId={}", event.getOrderId());
                return;
            }
            log.info("收到设计师分配事件: orderId={}, newDesignerId={}, oldDesignerId={}",
                    event.getOrderId(), event.getNewDesignerId(), event.getOldDesignerId());
            notificationService.send(event.getNewDesignerId(), NotificationDTO.builder()
                    .title("您有新的设计任务")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.DESIGN)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.DESIGN_LIST)
                    .build());
            log.info("已通知新设计师: orderId={}, newDesignerId={}", event.getOrderId(), event.getNewDesignerId());
            // 重新分配时通知原设计师任务已被撤销
            if (event.getOldDesignerId() != null) {
                notificationService.send(event.getOldDesignerId(), NotificationDTO.builder()
                        .title("您的设计任务已被重新分配")
                        .messageType(MessageTypeEnum.POPUP)
                        .category(MessageCategoryEnum.DESIGN)
                        .bizType(BizTypeEnum.ORDER.getCode())
                        .bizId(event.getOrderId())
                        .jumpUrl(NotificationJumpUrlConstants.DESIGN_LIST)
                        .build());
                log.info("已通知旧设计师任务重新分配: orderId={}, oldDesignerId={}", event.getOrderId(), event.getOldDesignerId());
            }
        } catch (Exception e) {
            log.error("设计师分配通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 订单修改申请提交事件
     * 通知全部设计管理员审核
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModifyApplySubmitted(OrderModifyApplySubmittedEvent event) {
        try {
            log.info("收到订单修改申请提交事件: orderId={}, applyUserId=",
                    event.getOrderId(), event.getApplyUserId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("有新的订单修改申请待审核")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL + event.getOrderId())
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(),
                    NotificationContext.all(), dto);
            log.info("已通知全部设计管理员修改申请: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单修改申请通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 订单修改申请驳回事件
     * 定向通知订单业务人员
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModifyApplyRejected(OrderModifyApplyRejectedEvent event) {
        try {
            log.info("收到订单修改申请驳回事件: applyId={}, operatorId={}", event.getApplyId(), event.getOperatorId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("您的订单修改申请已被驳回")
                    .content("驳回原因：" + event.getRejectReason())
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.ORDER)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getApplyId())
                    .build();
            notificationService.send(event.getOperatorId(), dto);
            log.info("已通知订单业务人员修改申请驳回: applyId={}, operatorId={}", event.getApplyId(), event.getOperatorId());
        } catch (Exception e) {
            log.error("订单修改申请驳回通知发送失败: applyId={}", event.getApplyId(), e);
        }
    }

    /**
     * 生产流转卡批量创建事件
     * 通知全部生产员和生产管理员（不限加工中心）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductionCardsCreated(ProductionCardsCreatedEvent event) {
        try {
            log.info("收到生产流转卡创建事件: recordCount={}, recordIds={}", event.getRecordIds().size(), event.getRecordIds());
            List<ProductionRecordEntity> records = productionRecordService.listByIds(event.getRecordIds());
            if (CollectionUtils.isEmpty(records)) {
                log.warn("流转卡记录不存在，跳过通知: recordIds={}", event.getRecordIds());
                return;
            }

            // 查询全部生产员和生产管理员（一次查询，所有流转卡共享）
            List<Long> workerIds = notificationService.resolveUserIds(
                RoleCodeEnum.PRODUCTION_WORKER.getCode(), NotificationContext.all());
            List<Long> managerIds = notificationService.resolveUserIds(
                RoleCodeEnum.PRODUCTION_MANAGER.getCode(), NotificationContext.all());
            log.info("生产用户列表: workerIds={}, managerIds={}", workerIds, managerIds);

            // 为每张流转卡发送通知
            for (ProductionRecordEntity record : records) {
                String bizData = JSONUtil.toJsonStr(Map.of(
                        "recordNo", record.getRecordNo(),
                        "orderId", record.getOrderId()
                ));
                NotificationDTO dto = NotificationDTO.builder()
                        .title("有新的生产流转卡待接收")
                        .messageType(MessageTypeEnum.POPUP)
                        .category(MessageCategoryEnum.PRODUCTION)
                        .bizType(BizTypeEnum.PRODUCTION_CARD.getCode())
                        .bizId(record.getId())
                        .bizData(bizData)
                        .jumpUrl(NotificationJumpUrlConstants.PRODUCTION_RECORD + record.getId())
                        .build();
                notificationService.send(workerIds, dto);
                notificationService.send(managerIds, dto);
            }
            log.info("生产流转卡通知已发送: recordCount={}, workerCount={}, managerCount={}",
                    records.size(), workerIds.size(), managerIds.size());
        } catch (Exception e) {
            log.error("生产流转卡通知发送失败: recordIds={}", event.getRecordIds(), e);
        }
    }

    /**
     * 生产流转卡被接收事件
     * 不推送新消息，仅将其他生产员的待接收通知标记为 CLAIMED + 已确认
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductionCardClaimed(ProductionCardClaimedEvent event) {
        try {
            log.info("收到流转卡接收事件: recordId={}, claimedByUserId={}", event.getRecordId(), event.getClaimedByUserId());
            // 批量更新其他生产员的通知状态为 CLAIMED，使其无需强制弹窗确认
            notificationMessageMapper.batchMarkClaimed(event.getRecordId(), event.getClaimedByUserId());
            log.info("流转卡通知已标记 CLAIMED: recordId={}, claimedByUserId={}", event.getRecordId(), event.getClaimedByUserId());
        } catch (Exception e) {
            log.error("流转卡接收通知更新失败: recordId={}", event.getRecordId(), e);
        }
    }
}
