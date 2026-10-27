package com.yigongbao.module.backup.cleanup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.cleanup.entity.CleanupSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** COS 清理快照数据库访问接口。 */
@Mapper
public interface CleanupSnapshotMapper extends BaseMapper<CleanupSnapshotEntity> {
    /** 通过不可变快照哈希读取快照，避免重复创建同一候选集合。 */
    @Select("SELECT * FROM cos_cleanup_snapshot WHERE snapshot_hash = #{hash} AND is_deleted = 0 LIMIT 1")
    CleanupSnapshotEntity selectByHash(@Param("hash") String hash);
}
