package com.yigongbao.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.workload.DesignerWorkloadVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表 Mapper
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Mapper
public interface OrderMainMapper extends BaseMapper<OrderMainEntity> {

    @Select("SELECT * FROM order_main WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    OrderMainEntity selectByIdForUpdate(@Param("id") Long id);

    /**
     * 统计设计师工作量
     *
     * @param createTimeStart 开始时间
     * @param createTimeEnd 结束时间
     * @return 设计师工作量统计列表
     */
    @Select("""
        SELECT
            om.designer_id AS designerId,
            u.real_name AS designerName,
            COUNT(*) AS caseCount,
            IFNULL(SUM(rp.points), 0) AS totalPoints
        FROM order_main om
        INNER JOIN order_item oi ON om.id = oi.order_id
        INNER JOIN rebuild_project rp ON oi.project_id = rp.id
        LEFT JOIN sys_user u ON om.designer_id = u.id
        WHERE om.designer_id IS NOT NULL
          AND om.status >= 2030
          AND om.is_deleted = 0
          AND (#{createTimeStart} IS NULL OR om.create_time >= #{createTimeStart})
          AND (#{createTimeEnd} IS NULL OR om.create_time <= #{createTimeEnd})
        GROUP BY om.designer_id, u.real_name
        ORDER BY totalPoints DESC, caseCount DESC
    """)
    List<DesignerWorkloadVO> statisticsDesignerWorkload(
            @Param("createTimeStart") LocalDateTime createTimeStart,
            @Param("createTimeEnd") LocalDateTime createTimeEnd
    );
}
