package com.yigongbao.module.notification.listener;

import cn.hutool.json.JSONUtil;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.event.*;
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
import java.util.stream.Collectors;

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
     * 试用订单 → 通知区域管理员（HOSPITALS scope）
     * 其他订单 → 通知设计管理员（ORG scope）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderSubmitted(OrderSubmittedEvent event) {
        try {
            log.info("收到订单提交事件: orderId={}, businessType={}, hospitalId={}, orgId={}",
                    event.getOrderId(), event.getBusinessType(), event.getHospitalId(), event.getOrgId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("有新的订单待审核")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl("/order/detail/" + event.getOrderId())
                    .build();
            // 试用订单（businessType=11.3）需区域管理员审核，其他订单直接由设计管理员审核
            if (DictCodeConstants.ORDER_BUSINESS_TYPE_TRIAL.equals(event.getBusinessType())) {
                log.info("试用订单，通知区域管理员: orderId={}, hospitalId={}", event.getOrderId(), event.getHospitalId());
                notificationService.send(RoleCodeEnum.REGIONAL_MANAGER.getCode(),
                        NotificationContext.ofHospital(event.getHospitalId()), dto);
            } else {
                log.info("普通订单，通知设计管理员: orderId={}, orgId={}", event.getOrderId(), event.getOrgId());
                notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(),
                        NotificationContext.ofOrg(event.getOrgId()), dto);
            }
        } catch (Exception e) {
            log.error("订单提交通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 区域审核通过事件（仅试用订单触发）
     * 通知设计管理员（ORG scope）分配设计师
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegionalAuditPassed(RegionalAuditPassedEvent event) {
        try {
            log.info("收到区域审核通过事件: orderId={}, orgId={}", event.getOrderId(), event.getOrgId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("试用订单区域审核已通过，请分配设计师")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl("/order/detail/" + event.getOrderId())
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(),
                    NotificationContext.ofOrg(event.getOrgId()), dto);
            log.info("已通知设计管理员区域审核通过: orderId={}, orgId={}", event.getOrderId(), event.getOrgId());
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
                    .messageType(MessageTypeEnum.MESSAGE)
                    .category(MessageCategoryEnum.ORDER)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl("/order/detail/" + event.getOrderId())
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
                    .jumpUrl("/design/list")
                    .build());
            log.info("已通知新设计师: orderId={}, newDesignerId={}", event.getOrderId(), event.getNewDesignerId());
            // 重新分配时通知原设计师任务已被撤销
            if (event.getOldDesignerId() != null) {
                notificationService.send(event.getOldDesignerId(), NotificationDTO.builder()
                        .title("您的设计任务已被重新分配")
                        .messageType(MessageTypeEnum.MESSAGE)
                        .category(MessageCategoryEnum.DESIGN)
                        .bizType(BizTypeEnum.ORDER.getCode())
                        .bizId(event.getOrderId())
                        .jumpUrl("/design/list")
                        .build());
                log.info("已通知旧设计师任务重新分配: orderId={}, oldDesignerId={}", event.getOrderId(), event.getOldDesignerId());
            }
        } catch (Exception e) {
            log.error("设计师分配通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 订单修改申请提交事件
     * 通知设计管理员（ORG scope）审核
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModifyApplySubmitted(OrderModifyApplySubmittedEvent event) {
        try {
            log.info("收到订单修改申请提交事件: orderId={}, applyUserId={}, orgId={}",
                    event.getOrderId(), event.getApplyUserId(), event.getOrgId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("有新的订单修改申请待审核")
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl("/order/detail/" + event.getOrderId())
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(),
                    NotificationContext.ofOrg(event.getOrgId()), dto);
            log.info("已通知设计管理员修改申请: orderId={}, orgId={}", event.getOrderId(), event.getOrgId());
        } catch (Exception e) {
            log.error("订单修改申请通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 订单修改申请驳回事件
     * 定向通知申请人
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModifyApplyRejected(OrderModifyApplyRejectedEvent event) {
        try {
            log.info("收到订单修改申请驳回事件: applyId={}, applyUserId={}", event.getApplyId(), event.getApplyUserId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("您的订单修改申请已被驳回")
                    .content("驳回原因：" + event.getRejectReason())
                    .messageType(MessageTypeEnum.MESSAGE)
                    .category(MessageCategoryEnum.ORDER)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getApplyId())
                    .build();
            notificationService.send(event.getApplyUserId(), dto);
            log.info("已通知申请人修改申请驳回: applyId={}, applyUserId={}", event.getApplyId(), event.getApplyUserId());
        } catch (Exception e) {
            log.error("订单修改申请驳回通知发送失败: applyId={}", event.getApplyId(), e);
        }
    }

    /**
     * 生产流转卡批量创建事件
     * 按加工中心分组，同一中心的用户列表只查一次（避免 N+1）
     * 分别通知 production-worker 和 production-manager（POPUP）
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
            // 按 centerId 分组，同一加工中心的用户列表只查一次
            Map<Long, List<ProductionRecordEntity>> byCenterId = records.stream()
                    .filter(r -> r.getProcessingCenterId() != null)
                    .collect(Collectors.groupingBy(ProductionRecordEntity::getProcessingCenterId));

            if (records.size() != byCenterId.values().stream().mapToInt(List::size).sum()) {
                log.error("流转卡 processingCenterId 为 null，已跳过: recordIds={}",
                    records.stream().filter(r -> r.getProcessingCenterId() == null)
                           .map(ProductionRecordEntity::getId).toList());
            }

            for (Map.Entry<Long, List<ProductionRecordEntity>> entry : byCenterId.entrySet()) {
                Long centerId = entry.getKey();
                NotificationContext ctx = NotificationContext.ofCenter(centerId);
                // 同一 centerId 下，WORKER 和 MANAGER 的用户列表各查一次，在该 center 所有流转卡间复用
                List<Long> workerIds = notificationService.resolveUserIds(RoleCodeEnum.PRODUCTION_WORKER.getCode(), ctx);
                List<Long> managerIds = notificationService.resolveUserIds(RoleCodeEnum.PRODUCTION_MANAGER.getCode(), ctx);
                log.info("加工中心用户列表解析: centerId={}, workerIds={}, managerIds={}", centerId, workerIds, managerIds);

                for (ProductionRecordEntity record : entry.getValue()) {
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
                            .jumpUrl("/production/record/" + record.getId())
                            .build();
                    notificationService.send(workerIds, dto);
                    notificationService.send(managerIds, dto);
                }
                log.info("生产流转卡通知已发送: centerId={}, recordCount={}, workerCount={}, managerCount={}",
                        centerId, entry.getValue().size(), workerIds.size(), managerIds.size());
            }
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
