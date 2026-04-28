package com.yigongbao.module.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门 Mapper，提供部门表的基础 CRUD 操作
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Mapper
public interface DeptMapper extends BaseMapper<DeptEntity> {
}
