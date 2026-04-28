package com.yigongbao.module.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 部门-机构关联 Mapper
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Mapper
public interface DeptOrgMapper extends BaseMapper<DeptOrgEntity> {
    List<Long> selectOrgIdsByDeptId(@Param("deptId") Long deptId);
    List<Long> selectDeptIdsByOrgId(@Param("orgId") Long orgId);
}
