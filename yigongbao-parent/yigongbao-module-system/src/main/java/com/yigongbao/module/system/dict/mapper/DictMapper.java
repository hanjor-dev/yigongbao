package com.yigongbao.module.system.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.system.dict.entity.DictEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 字典 Mapper
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Mapper
public interface DictMapper extends BaseMapper<DictEntity> {

    /** 获取根字典编码的 MySQL 命名锁。 */
    @Select("SELECT GET_LOCK(#{lockName}, #{timeoutSeconds})")
    Integer acquireRootCodeLock(@Param("lockName") String lockName,
                                @Param("timeoutSeconds") int timeoutSeconds);

    /** 释放根字典编码的 MySQL 命名锁。 */
    @Select("SELECT RELEASE_LOCK(#{lockName})")
    Integer releaseRootCodeLock(@Param("lockName") String lockName);

    /** 锁定父节点，避免同一父节点下的并发请求生成相同编码。 */
    @Select("""
            SELECT id
            FROM sys_dict
            WHERE id = #{parentId} AND is_deleted = 0
            FOR UPDATE
            """)
    Long lockCodeAllocation(@Param("parentId") Long parentId);

    /** 查询根节点编码的数值最大值。 */
    @Select("""
            SELECT MAX(CAST(dict_code AS UNSIGNED))
            FROM sys_dict
            WHERE parent_id = 0 AND is_deleted = 0
            """)
    Long selectMaxRootCode();

    /** 查询指定父节点下子编码最后一段的数值最大值。 */
    @Select("""
            SELECT MAX(CAST(SUBSTRING_INDEX(dict_code, '.', -1) AS UNSIGNED))
            FROM sys_dict
            WHERE parent_id = #{parentId} AND is_deleted = 0
            """)
    Long selectMaxChildSuffix(@Param("parentId") Long parentId);
}
