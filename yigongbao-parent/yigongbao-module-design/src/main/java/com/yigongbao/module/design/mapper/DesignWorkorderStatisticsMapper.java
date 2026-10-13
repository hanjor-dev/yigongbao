package com.yigongbao.module.design.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.design.vo.DesignWorkorderStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 设计工单统计 Mapper。 */
@Mapper
public interface DesignWorkorderStatisticsMapper extends BaseMapper<OrderMainEntity> {
    @Select("""
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN status = 2010 THEN 1 ELSE 0 END), 0) AS pendingDesign,
               COALESCE(SUM(CASE WHEN status = 2020 THEN 1 ELSE 0 END), 0) AS designing
        FROM order_main ${ew.customSqlSegment}
        """)
    DesignWorkorderStatisticsVO selectStatistics(@Param(Constants.WRAPPER) Wrapper<OrderMainEntity> wrapper);
}
