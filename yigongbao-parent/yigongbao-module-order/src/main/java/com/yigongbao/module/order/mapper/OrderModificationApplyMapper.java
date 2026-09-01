package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 订单修改申请 Mapper
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Mapper
public interface OrderModificationApplyMapper extends BaseMapper<OrderModificationApplyEntity> {

    /**
     * 将指定订单中已到期或订单阶段已变化的待审核申请实时标记为过期。
     */
    @Update("""
        UPDATE order_modification_apply a
        INNER JOIN order_main o ON o.id = a.order_id AND o.is_deleted = 0
        SET a.status = 4
        WHERE a.order_id = #{orderId}
          AND a.status = 1
          AND a.is_deleted = 0
          AND (a.expire_time IS NULL OR a.expire_time <= #{now}
               OR (a.apply_phase IS NOT NULL AND a.apply_phase <> o.phase))
    """)
    int expireApplicationsForOrder(@Param("orderId") Long orderId,
                                    @Param("now") LocalDateTime now);

    @Update("""
        UPDATE order_modification_apply a
        INNER JOIN order_main o ON o.id = a.order_id AND o.is_deleted = 0
        SET a.status = 4
        WHERE a.status = 1
          AND a.is_deleted = 0
          AND a.apply_phase IS NOT NULL
          AND a.apply_phase <> o.phase
    """)
    int expireApplicationsForChangedPhase();
}
