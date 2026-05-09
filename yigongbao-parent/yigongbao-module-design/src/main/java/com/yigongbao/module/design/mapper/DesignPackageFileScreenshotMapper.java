package com.yigongbao.module.design.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.design.entity.DesignPackageFileScreenshotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 数据包文件截图关联 Mapper
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Mapper
public interface DesignPackageFileScreenshotMapper extends BaseMapper<DesignPackageFileScreenshotEntity> {

    /**
     * 查询指定数据包下所有截图的最晚更新时间
     *
     * @param packageId 数据包ID
     * @return 最晚更新时间，无截图时返回 null
     */
    @Select("SELECT MAX(s.update_time) " +
            "FROM design_package_file_screenshot s " +
            "INNER JOIN design_package_file f ON s.package_file_id = f.id " +
            "WHERE f.package_id = #{packageId} AND s.is_deleted = 0")
    LocalDateTime getLatestUpdateTime(@Param("packageId") Long packageId);
}
