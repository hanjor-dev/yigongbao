package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.order.dto.order.CancelApplyPageQueryDTO;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单取消申请 Mapper
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Mapper
public interface OrderCancelApplyMapper extends BaseMapper<OrderCancelApplyEntity> {

    @Select({
            "<script>",
            "SELECT oca.* FROM order_cancel_apply oca",
            "LEFT JOIN order_main om ON om.id = oca.order_id AND om.is_deleted = 0",
            "LEFT JOIN sys_user applicant ON applicant.id = oca.apply_by AND applicant.is_deleted = 0",
            "WHERE oca.audit_status = 1 AND oca.is_deleted = 0",
            "<if test='query.orderCode != null and query.orderCode != \"\"'>AND (om.order_code LIKE CONCAT('%', #{query.orderCode}, '%') OR om.public_order_code LIKE CONCAT('%', #{query.orderCode}, '%'))</if>",
            "<if test='query.applyByName != null and query.applyByName != \"\"'>AND applicant.real_name LIKE CONCAT('%', #{query.applyByName}, '%')</if>",
            "<if test='query.applyBy != null'>AND oca.apply_by = #{query.applyBy}</if>",
            "<if test='query.createTimeStart != null'>AND oca.create_time &gt;= #{query.createTimeStart}</if>",
            "<if test='query.createTimeEnd != null'>AND oca.create_time &lt;= #{query.createTimeEnd}</if>",
            "ORDER BY oca.create_time DESC",
            "</script>"
    })
    IPage<OrderCancelApplyEntity> selectPendingPage(
            Page<OrderCancelApplyEntity> page,
            @Param("query") CancelApplyPageQueryDTO query);

    @Select({
            "<script>",
            "SELECT oca.* FROM order_cancel_apply oca",
            "LEFT JOIN order_main om ON om.id = oca.order_id AND om.is_deleted = 0",
            "LEFT JOIN sys_user applicant ON applicant.id = oca.apply_by AND applicant.is_deleted = 0",
            "WHERE oca.apply_by = #{applyBy} AND oca.is_deleted = 0",
            "<if test='query.orderCode != null and query.orderCode != \"\"'>AND (om.order_code LIKE CONCAT('%', #{query.orderCode}, '%') OR om.public_order_code LIKE CONCAT('%', #{query.orderCode}, '%'))</if>",
            "<if test='query.applyByName != null and query.applyByName != \"\"'>AND applicant.real_name LIKE CONCAT('%', #{query.applyByName}, '%')</if>",
            "<if test='query.applyBy != null'>AND oca.apply_by = #{query.applyBy}</if>",
            "<if test='query.createTimeStart != null'>AND oca.create_time &gt;= #{query.createTimeStart}</if>",
            "<if test='query.createTimeEnd != null'>AND oca.create_time &lt;= #{query.createTimeEnd}</if>",
            "ORDER BY oca.create_time DESC",
            "</script>"
    })
    IPage<OrderCancelApplyEntity> selectMyPage(
            Page<OrderCancelApplyEntity> page,
            @Param("query") CancelApplyPageQueryDTO query,
            @Param("applyBy") Long applyBy);
}
