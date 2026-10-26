package com.yigongbao.module.backup.candidate.mapper;

import com.yigongbao.common.entity.OrderMainEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 对账专用订单查询，显式绕过 MyBatis-Plus 逻辑删除条件。 */
@Mapper
public interface OrderBackupOrderMapper {

    @Select("SELECT * FROM order_main WHERE id BETWEEN #{orderIdFrom} AND #{orderIdTo} "
            + "ORDER BY id ASC")
    List<OrderMainEntity> selectRangeIncludingDeleted(@Param("orderIdFrom") Long orderIdFrom,
                                                       @Param("orderIdTo") Long orderIdTo);
}
