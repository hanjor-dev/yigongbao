package com.yigongbao.module.notification.mapper;

import com.yigongbao.module.notification.dto.CancelApplyNotificationData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 取消申请通知只读查询，保持通知模块与订单模块解耦。
 */
@Mapper
public interface CancelApplyQueryMapper {

    @Select("SELECT om.order_code AS orderCode, om.public_order_code AS publicOrderCode, oca.apply_reason AS applyReason, "
            + "oca.audit_reason AS auditReason "
            + "FROM order_cancel_apply oca "
            + "LEFT JOIN order_main om ON om.id = oca.order_id AND om.is_deleted = 0 "
            + "WHERE oca.id = #{applyId} AND oca.is_deleted = 0 LIMIT 1")
    CancelApplyNotificationData findByApplyId(@Param("applyId") Long applyId);
}
