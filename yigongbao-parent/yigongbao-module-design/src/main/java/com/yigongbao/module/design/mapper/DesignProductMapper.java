package com.yigongbao.module.design.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.design.entity.DesignProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 打印产品信息 Mapper
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Mapper
public interface DesignProductMapper extends BaseMapper<DesignProductEntity> {

    /**
     * 查询指定数据包下所有打印产品的最后修改时间（MAX update_time）
     * 用于判断打印信息是否在上次生成指令单/图纸之后发生过变化。
     * 若该包下无产品记录，返回 null。
     *
     * @param packageId 数据包ID
     * @return 最后修改时间，无记录时返回 null
     */
    @Select("SELECT MAX(update_time) FROM design_product WHERE package_id = #{packageId} AND is_deleted = 0")
    LocalDateTime getLatestUpdateTime(Long packageId);
}
