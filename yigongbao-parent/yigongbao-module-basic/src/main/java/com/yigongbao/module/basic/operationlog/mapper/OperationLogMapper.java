package com.yigongbao.module.basic.operationlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.basic.operationlog.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
