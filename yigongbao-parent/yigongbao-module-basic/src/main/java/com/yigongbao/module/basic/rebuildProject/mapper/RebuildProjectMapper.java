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

    /**
     * 根据分类编码读取字典名称，避免分类名称与字典配置脱节。
     */
    @Select("SELECT dict_name FROM sys_dict " +
            "WHERE dict_code = #{categoryCode} AND status = 1 AND is_deleted = 0 LIMIT 1")
    String selectCategoryName(@Param("categoryCode") String categoryCode);
}
