package com.yigongbao.module.backup.cleanup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.cleanup.entity.CleanupPermitEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** COS 删除授权数据库访问接口。 */
@Mapper
public interface CleanupPermitMapper extends BaseMapper<CleanupPermitEntity> {
    /** 查询任务当前有效的授权，数据库唯一索引同时提供最终并发保护。 */
    @Select("SELECT * FROM cos_delete_permit WHERE cleanup_task_id = #{taskId} "
            + "AND permit_status IN ('REQUESTED','ISSUED','CLAIMED') AND is_deleted = 0 LIMIT 1")
    CleanupPermitEntity selectActiveByTaskId(@Param("taskId") Long taskId);

    /** 将过期的有效授权原子标记为 EXPIRED。 */
    @Update("UPDATE cos_delete_permit SET permit_status = 'EXPIRED', version = version + 1, "
            + "update_time = CURRENT_TIMESTAMP(3) WHERE permit_id = #{permitId} "
            + "AND permit_status IN ('REQUESTED','ISSUED','CLAIMED') "
            + "AND expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP(3) AND is_deleted = 0")
    int expireIfNecessary(@Param("permitId") String permitId);

    /** 查询任务历史签发次数。 */
    @Select("SELECT COALESCE(MAX(attempt_no), 0) FROM cos_delete_permit "
            + "WHERE cleanup_task_id = #{taskId} AND is_deleted = 0")
    Integer selectMaxAttempt(@Param("taskId") Long taskId);

    /** 读取外部 permit 标识。 */
    @Select("SELECT * FROM cos_delete_permit WHERE permit_id = #{permitId} AND is_deleted = 0 LIMIT 1")
    CleanupPermitEntity selectByPermitId(@Param("permitId") String permitId);

    /** 仅允许从指定状态撤销授权。 */
    @Update("UPDATE cos_delete_permit SET permit_status = 'REVOKED', version = version + 1, "
            + "update_time = CURRENT_TIMESTAMP(3) WHERE permit_id = #{permitId} "
            + "AND permit_status IN ('REQUESTED','ISSUED','CLAIMED') AND is_deleted = 0")
    int revoke(@Param("permitId") String permitId);
}
