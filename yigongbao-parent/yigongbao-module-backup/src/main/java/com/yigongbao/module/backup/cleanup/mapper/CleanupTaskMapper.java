package com.yigongbao.module.backup.cleanup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.cleanup.entity.CleanupTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** COS 清理任务数据库访问接口。 */
@Mapper
public interface CleanupTaskMapper extends BaseMapper<CleanupTaskEntity> {
    /** 读取幂等键对应的既有任务。 */
    @Select("SELECT * FROM cos_cleanup_task WHERE idempotency_key = #{key} AND is_deleted = 0 LIMIT 1")
    CleanupTaskEntity selectByIdempotencyKey(@Param("key") String key);

    /** 通过外部任务号读取任务。 */
    @Select("SELECT * FROM cos_cleanup_task WHERE task_no = #{taskNo} AND is_deleted = 0 LIMIT 1")
    CleanupTaskEntity selectByTaskNo(@Param("taskNo") String taskNo);

    /** 按创建时间倒序返回任务列表。 */
    @Select("SELECT * FROM cos_cleanup_task WHERE is_deleted = 0 ORDER BY create_time DESC, id DESC")
    List<CleanupTaskEntity> selectAllActive();

    /** 使用版本号和原状态做条件更新，防止旧请求覆盖新状态。 */
    @Update("UPDATE cos_cleanup_task SET task_status = #{target}, version = version + 1, "
            + "update_time = CURRENT_TIMESTAMP(3) WHERE task_no = #{taskNo} "
            + "AND task_status = #{expected} AND version = #{version} AND is_deleted = 0")
    int updateStatus(@Param("taskNo") String taskNo, @Param("expected") String expected,
                     @Param("target") String target, @Param("version") Integer version);
}
