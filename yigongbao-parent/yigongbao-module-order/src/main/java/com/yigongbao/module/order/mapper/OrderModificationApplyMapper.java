package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 订单修改申请 Mapper
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Mapper
public interface OrderModificationApplyMapper extends BaseMapper<OrderModificationApplyEntity> {

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
