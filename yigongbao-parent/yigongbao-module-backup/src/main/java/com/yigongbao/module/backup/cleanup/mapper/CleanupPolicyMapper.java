package com.yigongbao.module.backup.cleanup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.cleanup.entity.CleanupPolicyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** COS 清理策略数据库访问接口。 */
@Mapper
public interface CleanupPolicyMapper extends BaseMapper<CleanupPolicyEntity> {
    /** 读取同一策略版本，避免并发创建违反唯一键。 */
    @Select("SELECT * FROM cos_cleanup_policy WHERE policy_code = #{code} "
            + "AND policy_version = #{version} AND is_deleted = 0 LIMIT 1")
    CleanupPolicyEntity selectByCodeVersion(@Param("code") String code, @Param("version") Integer version);
}
