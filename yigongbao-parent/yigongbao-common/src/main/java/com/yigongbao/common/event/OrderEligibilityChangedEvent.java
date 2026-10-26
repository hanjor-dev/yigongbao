package com.yigongbao.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 订单备份资格生命周期事件。
 *
 * <p>事件只携带稳定的订单标识和状态，不依赖备份模块的业务 Service，
 * 从而避免订单模块与备份模块形成 Maven 循环依赖。事件由订单状态事务发布，
 * 备份模块在 {@code BEFORE_COMMIT} 阶段同步落库候选事实。</p>
 */
@Getter
public class OrderEligibilityChangedEvent extends ApplicationEvent {

    /** 订单正式、未取消且未逻辑删除，进入新的备份资格代次。 */
    public static final String FORMAL = "FORMAL";
    /** 兼容候选表中的状态命名；FORMAL 与 ACTIVE 表示相同资格状态。 */
    public static final String ACTIVE = "ACTIVE";
    /** 订单撤回到草稿，当前备份候选关闭。 */
    public static final String DRAFT = "DRAFT";
    /** 订单取消，当前备份候选不再启动新的备份任务。 */
    public static final String CANCELLED = "CANCELLED";
    /** 订单逻辑删除，当前备份候选进入删除终态。 */
    public static final String DELETED = "DELETED";

    private final Long orderId;
    private final String targetState;
    private final LocalDateTime occurredAt;

    public OrderEligibilityChangedEvent(Object source, Long orderId, String targetState,
                                        LocalDateTime occurredAt) {
        super(source);
        if (orderId == null || targetState == null || targetState.isBlank() || occurredAt == null) {
            throw new IllegalArgumentException("订单资格事件参数不能为空");
        }
        this.orderId = orderId;
        this.targetState = targetState;
        this.occurredAt = occurredAt;
    }
}
