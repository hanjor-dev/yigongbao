package com.yigongbao.module.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.vo.DeptStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门 Mapper，提供部门表的基础 CRUD 操作
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Mapper
public interface DeptMapper extends BaseMapper<DeptEntity> {

    @Select("""
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN dept_type = '6.1' THEN 1 ELSE 0 END), 0) AS enterprise,
               COALESCE(SUM(CASE WHEN dept_type = '6.2' THEN 1 ELSE 0 END), 0) AS business
        FROM sys_dept ${ew.customSqlSegment}
        """)
    DeptStatisticsVO selectStatistics(@Param(Constants.WRAPPER) Wrapper<DeptEntity> wrapper);
}
