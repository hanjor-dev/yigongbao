package com.yigongbao.module.basic.rebuildProject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.rebuildProject.entity.RebuildProjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 重建项目 Mapper
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Mapper
public interface RebuildProjectMapper extends BaseMapper<RebuildProjectEntity> {

    /**
     * 统计引用该项目的订单明细数量（含草稿）
     *
     * @param projectId 项目ID
     * @return 引用数量
     */
    @Select("SELECT (SELECT COUNT(*) FROM order_item WHERE project_id = #{projectId} AND is_deleted = 0) + " +
            "(SELECT COUNT(*) FROM order_item_draft WHERE project_id = #{projectId} AND is_deleted = 0)")
    Long countOrderItemReferences(@Param("projectId") Long projectId);
}
