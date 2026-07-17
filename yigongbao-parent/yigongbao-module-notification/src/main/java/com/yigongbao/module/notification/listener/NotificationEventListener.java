package com.yigongbao.module.notification.listener;

import cn.hutool.json.JSONUtil;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.event.*;
import com.yigongbao.module.notification.constant.NotificationJumpUrlConstants;
import com.yigongbao.module.notification.dto.CancelApplyNotificationData;
import com.yigongbao.module.notification.dto.NotificationContext;
import com.yigongbao.module.notification.dto.NotificationDTO;
import com.yigongbao.module.notification.dto.NotificationField;
import com.yigongbao.module.notification.enums.BizTypeEnum;
import com.yigongbao.module.notification.enums.MessageCategoryEnum;
import com.yigongbao.module.notification.enums.MessageTypeEnum;
import com.yigongbao.module.notification.mapper.NotificationMessageMapper;
import com.yigongbao.module.notification.mapper.CancelApplyQueryMapper;
import com.yigongbao.module.notification.service.INotificationService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 消息通知事件监听器
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
    private final CancelApplyQueryMapper cancelApplyQueryMapper;
    private final IProductionRecordService productionRecordService;
    private final com.yigongbao.module.system.user.service.UserService userService;

    // ==================== 消息内容构建工具 ====================

    /** 构建卡片消息内容 JSON，包含字段列表和空备注 */
    private static String content(NotificationField... fields) {
        return JSONUtil.toJsonStr(Map.of("fields", List.of(fields), "remark", ""));
    }

    /** 构建消息卡片字段：标识、中文名、值 */
    private static NotificationField f(String key, String label, String value) {
        return new NotificationField(key, label, value != null ? value : "");
    }

    // ==================== 订单相关通知 ====================

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
                    .content(content(
                            f("orderCode", "订单号", event.getOrderCode()),
                            f("patientName", "患者", event.getPatientName()),
                            f("orgName", "机构", event.getOrgName()),
                            f("operatorName", "业务员", event.getOperatorName())))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL)
                    .build();
            if (DictCodeConstants.ORDER_BUSINESS_TYPE_TRIAL.equals(event.getBusinessType())) {
                log.info("试用订单，通知区域管理员: orderId={}, deptId={}", event.getOrderId(), event.getDeptId());
                notificationService.send(RoleCodeEnum.REGIONAL_MANAGER.getCode(),
                        NotificationContext.ofDept(event.getDeptId()), dto);
            } else {
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
            String auditorName = "";
            if (event.getRegionalAuditBy() != null) {
                var user = userService.getById(event.getRegionalAuditBy());
                if (user != null) {
                    auditorName = user.getRealName();
                }
            }
            NotificationDTO dto = NotificationDTO.builder()
                    .title("试用订单区域审核已通过，请您及时处理")
                    .content(content(
                            f("orderCode", "订单号", event.getOrderCode()),
                            f("patientName", "患者", event.getPatientName()),
                            f("orgName", "机构", event.getOrgName()),
                            f("regionalAuditor", "区域审核员", auditorName)))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL)
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(), NotificationContext.all(), dto);
            log.info("已通知全部设计管理员区域审核通过: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("区域审核通过通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 审核驳回事件，定向通知订单创建人
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuditRejected(AuditRejectedEvent event) {
        try {
            log.info("收到审核驳回事件: orderId={}, createBy={}", event.getOrderId(), event.getCreateBy());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("您的订单审核未通过")
                    .content(content(
                            f("orderCode", "订单号", event.getOrderCode()),
                            f("patientName", "患者", event.getPatientName()),
                            f("hospitalName", "医院", event.getHospitalName()),
                            f("rejectReason", "驳回原因", event.getRejectReason())))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.ORDER)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL)
                    .build();
            notificationService.send(event.getCreateBy(), dto);
            log.info("已通知订单创建人审核驳回: orderId={}, createBy={}", event.getOrderId(), event.getCreateBy());
        } catch (Exception e) {
            log.error("审核驳回通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 设计师分配事件，通知新设计师；重新分配时同时通知旧设计师
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
            String orderContent = content(
                    f("orderCode", "订单号", event.getOrderCode()),
                    f("patientName", "患者", event.getPatientName()),
                    f("hospitalName", "医院", event.getHospitalName()));
            notificationService.send(event.getNewDesignerId(), NotificationDTO.builder()
                    .title("您有新的设计任务")
                    .content(orderContent)
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.DESIGN)
                    .bizType(BizTypeEnum.ORDER.getCode())
                    .bizId(event.getOrderId())
                    .jumpUrl(NotificationJumpUrlConstants.DESIGN_LIST)
                    .build());
            log.info("已通知新设计师: orderId={}, newDesignerId={}", event.getOrderId(), event.getNewDesignerId());
            if (event.getOldDesignerId() != null) {
                notificationService.send(event.getOldDesignerId(), NotificationDTO.builder()
                        .title("您的设计任务已被重新分配")
                        .content(orderContent)
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
     * 订单修改申请提交事件，通知全部设计管理员
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModifyApplySubmitted(OrderModifyApplySubmittedEvent event) {
        try {
            log.info("收到订单修改申请提交事件: orderId={}, applyUserId={}", event.getOrderId(), event.getApplyUserId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("有新的订单修改申请待审核")
                    .content(content(
                            f("orderCode", "订单号", event.getOrderCode()),
                            f("applyUserName", "申请人", event.getApplyUserName())))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.MODIFY_APPLY.getCode())
                    .bizId(event.getApplyId())
                    .jumpUrl(NotificationJumpUrlConstants.MODIFY_APPLY)
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(), NotificationContext.all(), dto);
            log.info("已通知全部设计管理员修改申请: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("订单修改申请通知发送失败: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 订单修改申请驳回事件，定向通知订单业务人员
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModifyApplyRejected(OrderModifyApplyRejectedEvent event) {
        try {
            log.info("收到订单修改申请驳回事件: applyId={}, operatorId={}", event.getApplyId(), event.getOperatorId());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("您的订单修改申请已被驳回")
                    .content(content(
                            f("orderCode", "订单号", event.getOrderCode()),
                            f("rejectReason", "驳回原因", event.getRejectReason())))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.ORDER)
                    .bizType(BizTypeEnum.MODIFY_APPLY.getCode())
                    .bizId(event.getApplyId())
                    .jumpUrl(NotificationJumpUrlConstants.MODIFY_APPLY)
                    .build();
            notificationService.send(event.getOperatorId(), dto);
            log.info("已通知订单业务人员修改申请驳回: applyId={}, operatorId={}", event.getApplyId(), event.getOperatorId());
        } catch (Exception e) {
            log.error("订单修改申请驳回通知发送失败: applyId={}", event.getApplyId(), e);
        }
    }

    // ==================== 订单取消申请通知 ====================

    /** 取消申请提交后，通知全部设计管理员。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelApplySubmitted(CancelApplySubmittedEvent event) {
        try {
            CancelApplyNotificationData data = cancelApplyQueryMapper.findByApplyId(event.getApplyId());
            if (data == null) {
                log.warn("取消申请不存在，跳过审核通知: applyId={}", event.getApplyId());
                return;
            }
            String applicantName = userService.getUserRealName(event.getApplyBy());
            NotificationDTO dto = NotificationDTO.builder()
                    .title("有新的订单取消申请待审核")
                    .content(content(
                            f("orderCode", "订单号", data.getOrderCode()),
                            f("applicant", "申请人", applicantName),
                            f("applyReason", "取消原因", data.getApplyReason())))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.CANCEL_APPLY.getCode())
                    .bizId(event.getApplyId())
                    .jumpUrl(NotificationJumpUrlConstants.CANCEL_APPLY_AUDIT)
                    .build();
            notificationService.send(RoleCodeEnum.DESIGNER_MANAGER.getCode(), NotificationContext.all(), dto);
            log.info("已通知设计管理员审核取消申请: applyId={}, orderId={}",
                    event.getApplyId(), event.getOrderId());
        } catch (Exception e) {
            log.error("取消申请提交通知发送失败: applyId={}, orderId={}",
                    event.getApplyId(), event.getOrderId(), e);
        }
    }

    /** 取消申请审核通过后，通知申请人。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelApplyApproved(CancelApplyApprovedEvent event) {
        sendCancelApplyResultNotification(event.getApplyId(), event.getOrderId(), event.getApplyBy(),
                event.getAuditBy(), true);
    }

    /** 取消申请审核驳回后，通知申请人。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelApplyRejected(CancelApplyRejectedEvent event) {
        sendCancelApplyResultNotification(event.getApplyId(), event.getOrderId(), event.getApplyBy(),
                event.getAuditBy(), false);
    }

    private void sendCancelApplyResultNotification(Long applyId, Long orderId, Long applyBy,
                                                   Long auditBy, boolean approved) {
        try {
            CancelApplyNotificationData data = cancelApplyQueryMapper.findByApplyId(applyId);
            if (data == null || applyBy == null) {
                log.warn("取消申请结果通知数据不完整，跳过发送: applyId={}, applyBy={}", applyId, applyBy);
                return;
            }
            String auditorName = userService.getUserRealName(auditBy);
            String title = approved ? "订单取消申请已通过" : "订单取消申请已驳回";
            String result = approved ? "您的订单取消申请已通过，订单已取消" : "您的订单取消申请已被驳回";
            NotificationDTO dto = NotificationDTO.builder()
                    .title(title)
                    .content(content(
                            f("orderCode", "订单号", data.getOrderCode()),
                            f("auditor", "审核人", auditorName),
                            f("result", "审核结果", result),
                            f("auditReason", "审核意见", data.getAuditReason())))
                    .messageType(MessageTypeEnum.POPUP)
                    .category(approved ? MessageCategoryEnum.ORDER : MessageCategoryEnum.APPROVAL)
                    .bizType(BizTypeEnum.CANCEL_APPLY.getCode())
                    .bizId(applyId)
                    .jumpUrl(NotificationJumpUrlConstants.ORDER_DETAIL)
                    .build();
            notificationService.send(applyBy, dto);
            log.info("已通知取消申请人审核结果: applyId={}, orderId={}, approved={}",
                    applyId, orderId, approved);
        } catch (Exception e) {
            log.error("取消申请结果通知发送失败: applyId={}, orderId={}", applyId, orderId, e);
        }
    }

    // ==================== 生产相关通知 ====================

    /**
     * 生产流转卡批量创建事件，通知全部生产员和生产管理员
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
            List<Long> workerIds = notificationService.resolveUserIds(
                    RoleCodeEnum.PRODUCTION_WORKER.getCode(), NotificationContext.all());
            List<Long> managerIds = notificationService.resolveUserIds(
                    RoleCodeEnum.PRODUCTION_MANAGER.getCode(), NotificationContext.all());
            List<Long> allRecipients = new ArrayList<>(workerIds);
            allRecipients.addAll(managerIds);
            log.info("生产用户列表: workerIds={}, managerIds={}", workerIds, managerIds);
            for (ProductionRecordEntity record : records) {
                NotificationDTO dto = NotificationDTO.builder()
                        .title("有新的生产流转卡待接收")
                        .content(content(
                                f("recordNo", "流转卡编号", record.getRecordNo()),
                                f("orderCode", "订单号", record.getOrderCode()),
                                f("patientName", "患者", record.getPatientName()),
                                f("hospitalName", "医院", record.getHospitalName())))
                        .messageType(MessageTypeEnum.POPUP)
                        .category(MessageCategoryEnum.PRODUCTION)
                        .bizType(BizTypeEnum.PRODUCTION_CARD.getCode())
                        .bizId(record.getId())
                        .bizData(JSONUtil.toJsonStr(Map.of("recordNo", record.getRecordNo(), "orderId", record.getOrderId())))
                        .jumpUrl(NotificationJumpUrlConstants.PRODUCTION_RECORD)
                        .build();
                notificationService.send(allRecipients, dto);
            }
            log.info("生产流转卡通知已发送: recordCount={}, workerCount={}, managerCount={}",
                    records.size(), workerIds.size(), managerIds.size());
        } catch (Exception e) {
            log.error("生产流转卡通知发送失败: recordIds={}", event.getRecordIds(), e);
        }
    }

    /**
     * 生产流转卡被接收事件
     * 标记其他生产员的通知为 CLAIMED，并更新备注提示已被接收
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductionCardClaimed(ProductionCardClaimedEvent event) {
        try {
            log.info("收到流转卡接收事件: recordId={}, claimedByUserId={}", event.getRecordId(), event.getClaimedByUserId());
            notificationMessageMapper.batchMarkClaimed(event.getRecordId(), event.getClaimedByUserId());
            String name = event.getClaimedByUserName() != null ? event.getClaimedByUserName() : "他人";
            notificationService.updateRemark(BizTypeEnum.PRODUCTION_CARD.getCode(), event.getRecordId(),
                    MessageCategoryEnum.PRODUCTION.getCode(), "该流转卡已被" + name + "接收");
            log.info("流转卡通知已标记 CLAIMED 并更新备注: recordId={}", event.getRecordId());
        } catch (Exception e) {
            log.error("流转卡接收通知更新失败: recordId={}", event.getRecordId(), e);
        }
    }

    /**
     * 通知备注更新事件（订单审核通过/驳回后，更新原推送通知的备注）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationRemarkUpdate(NotificationRemarkUpdateEvent event) {
        try {
            notificationService.updateRemark(event.getBizType(), event.getBizId(), event.getCategory(), event.getRemark());
            log.info("通知备注已更新: bizType={}, bizId={}, remark={}", event.getBizType(), event.getBizId(), event.getRemark());
        } catch (Exception e) {
            log.error("通知备注更新失败: bizType={}, bizId={}", event.getBizType(), event.getBizId(), e);
        }
    }
}
