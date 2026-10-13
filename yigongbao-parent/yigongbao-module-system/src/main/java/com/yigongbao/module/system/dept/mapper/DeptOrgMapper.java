package com.yigongbao.module.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.dept.entity.DeptOrgEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门-机构关联 Mapper，提供部门与机构关联表的基础 CRUD 操作
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Mapper
public interface DeptOrgMapper extends BaseMapper<DeptOrgEntity> {
}
