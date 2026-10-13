package com.yigongbao.module.system.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.vo.OrgStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 机构 Mapper
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Mapper
public interface OrgMapper extends BaseMapper<OrgEntity> {

    @Select("""
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN org_type = '1.2' THEN 1 ELSE 0 END), 0) AS distributor,
               COALESCE(SUM(CASE WHEN org_type = '1.4' THEN 1 ELSE 0 END), 0) AS serviceProvider,
               COALESCE(SUM(CASE WHEN org_type = '1.3' THEN 1 ELSE 0 END), 0) AS medicalInstitution
        FROM sys_org ${ew.customSqlSegment}
        """)
    OrgStatisticsVO selectStatistics(@Param(Constants.WRAPPER) Wrapper<OrgEntity> wrapper);
}
