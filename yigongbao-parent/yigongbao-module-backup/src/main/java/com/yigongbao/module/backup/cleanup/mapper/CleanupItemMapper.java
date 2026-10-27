package com.yigongbao.module.backup.cleanup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.cleanup.entity.CleanupItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** COS 清理对象明细数据库访问接口。 */
@Mapper
public interface CleanupItemMapper extends BaseMapper<CleanupItemEntity> {
    /** 查询任务中的全部对象明细。 */
    @Select("SELECT * FROM cos_cleanup_item WHERE cleanup_task_id = #{taskId} AND is_deleted = 0 "
            + "ORDER BY id ASC")
    List<CleanupItemEntity> selectByTaskId(@Param("taskId") Long taskId);
}
